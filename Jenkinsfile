pipeline {
  agent {
    docker {
      image 'ubuntu:latest'
    }

  }
  stages {
    stage('setup java') {
      steps {
        sh '''apt update
apt install -y openjdk-11-jdk'''
        sh '''java --version
javac --version'''
      }
    }

    stage('build') {
      steps {
        sh './gradlew build'
      }
    }

  }
}