pipeline {
	options {
		timeout(time: 60, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label "centos-8-8gb"
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
					archiveArtifacts artifacts: '.*log,*/target/work/data/.metadata/.*log,*/tests/target/work/data/.metadata/.*log,apiAnalyzer-workspace/.metadata/.*log', allowEmptyArchive: true
					junit '**/target/surefire-reports/TEST-*.xml'
					discoverGitReferenceBuild referenceJob: 'eclipse.platform/master'
					recordIssues publishAllIssues:false, ignoreQualityGate:true, tool: eclipse(name: 'Compiler and API Tools', pattern: '**/target/compilelogs/*.xml'), qualityGates: [[threshold: 1, type: 'DELTA', unstable: true]]
					recordIssues publishAllIssues:false, tools: [mavenConsole(), javaDoc()]
				}
			}
		}
	}
}
