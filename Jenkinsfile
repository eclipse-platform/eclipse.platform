pipeline {
	options {
		timeout(time: 40, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label "centos-latest"
	}
	environment {
		MAVEN_OPTS = "-Dbuild.sysclasspath=ignore -Dincludeantruntime=false"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'openjdk-jdk17-latest'
	}
	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh """
					mvn clean verify --batch-mode --fail-at-end -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-Pbuild-individual-bundles -Pbree-libs -Papi-check \
						-DskipTests=false -Dcompare-version-with-baselines.skip=false \
						-Dproject.build.sourceEncoding=UTF-8 \
						-Dbuild.sysclasspath=ignore -Dincludeantruntime=false
					"""
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '*.log,*/target/work/data/.metadata/*.log,*/tests/target/work/data/.metadata/*.log,apiAnalyzer-workspace/.metadata/*.log', allowEmptyArchive: true
					junit '**/target/surefire-reports/TEST-*.xml'
					publishIssues issues:[scanForIssues(tool: java()), scanForIssues(tool: mavenConsole())]
				}
				failure {
					script {
						if (env.BRANCH_NAME == 'master') {
				            emailext body: "Please go to <a href='${BUILD_URL}console'>${BUILD_URL}console</a> and check the build failure.<br><br>",
				            subject: "eclipse.platform.ua  master build - BUILD FAILED", 
				            to: "platform-dev@eclipse.org	",
				            from:"genie.releng@eclipse.org"
			            }
		            }
		        }
			}
		}
	}
}
