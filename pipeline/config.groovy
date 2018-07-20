app {
    name = 'mds'
    version = 'snapshot'
    namespaces { //can't call environments :(
        'build'{
            namespace = 'empr-mds-tools'
            disposable = true
        }
        'dev' {
            namespace = 'empr-mds-dev'
            disposable = true
        }
        'test' {
            namespace = 'empr-mds-test'
            disposable = false
        }
        'prod' {
            namespace = 'empr-mds-prod'
            disposable = false
        }
    }

    git {
        workDir = ['git', 'rev-parse', '--show-toplevel'].execute().text.trim()
        uri = ['git', 'config', '--get', 'remote.origin.url'].execute().text.trim()
        commit = ['git', 'rev-parse', 'HEAD'].execute().text.trim()
        //ref = opt.'branch'?:['bash','-c', 'git config branch.`git name-rev --name-only HEAD`.merge'].execute().text.trim()
        changeId = "${opt.'pr'}"
        ref = opt.'branch'?:"refs/pull/${git.changeId}/head"
        github {
            owner = app.git.uri.tokenize('/')[2]
            name = app.git.uri.tokenize('/')[3].tokenize('.git')[0]
        }
    }

    build {
        name = "build-pr-${app.git.changeId}"
        prefix = "${app.name}-"
        suffix = "-${app.git.changeId}"
        namespace = 'empr-mds-tools'
        timeoutInSeconds = 60*20 // 20 minutes
        templates = [
                [
                    'file':'openshift/_nodejs.bc.json',
                    'params':[
                        'NAME':"mds-backend",
                        'SUFFIX': "${app.build.suffix}",
                        'OUTPUT_TAG_NAME':"build-pr-${app.git.changeId}",
                        'SOURCE_CONTEXT_DIR': "backend",
                        'SOURCE_REPOSITORY_URL': "${app.git.uri}"
                    ]
                ],
                [
                    'file':'openshift/_nodejs.bc.json',
                    'params':[
                        'NAME':"mds-frontend",
                        'SUFFIX': "${app.build.suffix}",
                        'OUTPUT_TAG_NAME':"build-pr-${app.git.changeId}",
                        'SOURCE_CONTEXT_DIR': "frontend",
                        'SOURCE_REPOSITORY_URL': "${app.git.uri}"
                    ]
                ],
                [
                    'file':'openshift/postgresql.bc.json',
                    'params':[
                        'NAME':"mds-postgresql",
                        'SUFFIX': "${app.build.suffix}",
                        'TAG_NAME':"build-pr-${app.git.changeId}"
                    ]
                ]
        ]
    }

    deployment {
        name = "${vars.deployment.key}-pr-${app.git.changeId}"
        prefix = "${app.name}-"
        suffix = "-${app.git.changeId}"
        namespace = "${vars.deployment.namespace}"
        timeoutInSeconds = 60*20 // 20 minutes
        templates = [
                [
                    'file':'openshift/_nodejs.dc.json',
                    'params':[
                        'NAME':"mds-backend",
                        'SUFFIX': "${app.deployment.suffix}",
                        'TAG_NAME':"${app.deployment.name}",
                        'APPLICATION_DOMAIN': "${vars.modules.'mds-backend'.HOST}"
                    ]
                ],
                [
                    'file':'openshift/_nodejs.dc.json',
                    'params':[
                        'NAME':"mds-frontend",
                        'SUFFIX': "${app.deployment.suffix}",
                        'TAG_NAME':"${app.deployment.name}",
                        'APPLICATION_DOMAIN': "${vars.modules.'mds-frontend'.HOST}"
                    ]
                ],
                [
                    'file':'openshift/postgresql.dc.json',
                    'params':[
                        'DATABASE_SERVICE_NAME':"mds-postgresql${app.deployment.suffix}",
                        'IMAGE_STREAM_NAMESPACE':'',
                        'IMAGE_STREAM_NAME':"mds-postgresql",
                        'IMAGE_STREAM_VERSION':"${app.deployment.name}",
                        'POSTGRESQL_DATABASE':'mds',
                        'VOLUME_CAPACITY':"${vars.DB_PVC_SIZE}"
                    ]
                ]
        ]
    }
}

//Default Values (Should it default to DEV or PROD???)
vars {
    DB_PVC_SIZE = '1Gi'
    modules {
        'mds-backend' {
            HOST = "mds-backend-${vars.git.changeId}-${vars.deployment.namespace}.pathfinder.gov.bc.ca"
        }
    }
}

environments {
    'dev' {
        vars {
            DB_PVC_SIZE = '1Gi'
            git {
                changeId = "${opt.'pr'}"
            }
            deployment {
                key = 'dev'
                namespace = 'empr-mds-dev'
            }
            modules {
                'mds-backend' {
                    HOST = "mds-backend-${vars.git.changeId}-${vars.deployment.namespace}.pathfinder.gov.bc.ca"
                }
                'mds-frontend' {
                    HOST = "mds-frontend-${vars.git.changeId}-${vars.deployment.namespace}.pathfinder.gov.bc.ca"
                }
            }
        }
    }
    'test' {
        vars {
            deployment {
                key = 'test'
                namespace = 'empr-mds-test'
            }
        }
    }
    'prod' {
        vars {
            deployment {
                key = 'prod'
                namespace = 'empr-mds-prod'
            }
        }
    }
}