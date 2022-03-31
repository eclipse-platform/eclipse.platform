pipeline {
	options {
		timeout(time: 40, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label "centos-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'openjdk-jdk11-latest'
	}
	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh """
					mvn clean verify --batch-mode --fail-at-end -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-Pbuild-individual-bundles -Pbree-libs -Papi-check \
						-DskipTests=false -Dcompare-version-with-baselines.skip=false \
						-Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true \
						-Dmaven.compiler.failOnWarning=true -Dproject.build.sourceEncoding=UTF-8 
					"""
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '.*log,*/target/work/data/.metadata/.*log,*/tests/target/work/data/.metadata/.*log,apiAnalyzer-workspace/data/.metadata/.*log'
					junit '**/target/surefire-reports/TEST-*.xml'
					publishIssues issues:[scanForIssues(tool: java()), scanForIssues(tool: mavenConsole())]
				}
				
			}
		}
		stage('Check freeze period') {
			when {
				not {
					branch 'master'
				}
			}
			steps {
				sh "wget https://download.eclipse.org/eclipse/relengScripts/scripts/verifyFreezePeriod.sh"
				sh "chmod +x verifyFreezePeriod.sh"
				withCredentials([string(credentialsId: 'google-api-key', variable: 'GOOGLE_API_KEY')]) {
					sh './verifyFreezePeriod.sh'
				}
			}
		}
	}
}
