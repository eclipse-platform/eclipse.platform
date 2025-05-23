pipeline {
	options {
		timeout(time: 60, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label "ubuntu-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk21-latest'
	}
	environment {
		MAVEN_OPTS = '-Xmx1500m'
	}
	stages {
		stage('Build') {
			steps {
				xvnc(useXauthority: true) {
					sh """
					mvn clean verify --batch-mode --fail-at-end -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-Pbree-libs -Papi-check -Pjavadoc \
						-Dmaven.test.failure.ignore=true \
						-Dcompare-version-with-baselines.skip=false \
						-Dmaven.compiler.failOnWarning=false \
						-Dorg.slf4j.simpleLogger.showDateTime=true -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss.SSS \
						-DtrimStackTrace=false
					"""
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '.*log,**/target/**/.*log', allowEmptyArchive: true
					junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
					discoverGitReferenceBuild referenceJob: 'eclipse.platform/master'
					recordIssues enabledForFailure: true, publishAllIssues:false, ignoreQualityGate:true,
						tools: [
							eclipse(name: 'Compiler', pattern: '**/target/compilelogs/*.xml'),
							javaDoc(),
							issues(name: 'API Tools', id: 'apitools', pattern: '**/target/apianalysis/*.xml')
						],
						qualityGates: [[threshold: 1, type: 'DELTA', unstable: true]]
					recordIssues enabledForFailure: true, publishAllIssues:false, tools: [mavenConsole()]
				}
			}
		}
	}
}
