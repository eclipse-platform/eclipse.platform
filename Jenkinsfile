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
					junit '**/target/surefire-reports/TEST-*.xml'
					discoverGitReferenceBuild referenceJob: 'eclipse.platform/master'
					recordIssues(publishAllIssues:false, ignoreQualityGate:true,
						tool: eclipse(name: 'Compiler and API Tools', pattern: '**/target/compilelogs/*.xml'),
						qualityGates: [[threshold: 1, type: 'DELTA', unstable: true]])
					recordIssues publishAllIssues:false, tools: [mavenConsole(), javaDoc()]
				}
			}
		}
	}
}
