// Simple Test Pipeline for Email Notifications
// Create a new Pipeline job in Jenkins and paste this script

pipeline {
    agent any

    stages {
        stage('Test Email') {
            steps {
                echo 'Testing email notification system...'
                echo 'Build number: ${BUILD_NUMBER}'
                echo 'Job name: ${JOB_NAME}'
            }
        }
    }

    post {
        always {
            script {
                echo "===== EMAIL TEST START ====="

                // Test 1: Try emailext (HTML)
                try {
                    emailext (
                        subject: "TEST Email from Jenkins - Build #${env.BUILD_NUMBER}",
                        body: """
                            <h2>This is a TEST email from Jenkins</h2>
                            <p>If you received this, emailext is working!</p>
                            <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                            <p><strong>Build:</strong> ${env.BUILD_NUMBER}</p>
                            <p><strong>Time:</strong> ${new Date()}</p>
                        """,
                        to: "mohammad.kheirkhah@gritlab.ax",
                        mimeType: 'text/html'
                    )
                    echo "✅ emailext sent successfully"
                } catch (Exception e) {
                    echo "❌ emailext failed: ${e.message}"

                    // Test 2: Try simple mail
                    try {
                        mail to: 'mohammad.kheirkhah@gritlab.ax',
                             subject: "TEST Email from Jenkins - Build #${env.BUILD_NUMBER}",
                             body: """
This is a TEST email from Jenkins

If you received this, simple mail is working!

Job: ${env.JOB_NAME}
Build: ${env.BUILD_NUMBER}
Time: ${new Date()}

---
If you see this, your email configuration is working!
                             """
                        echo "✅ Simple mail sent successfully"
                    } catch (Exception e2) {
                        echo "❌ Simple mail failed: ${e2.message}"
                        echo ""
                        echo "Email configuration is not working. Please check:"
                        echo "1. SMTP settings in Jenkins System Configuration"
                        echo "2. Gmail App Password is correct"
                        echo "3. Spam folder"
                        echo "4. See EMAIL_SETUP.md for troubleshooting"
                    }
                }

                echo "===== EMAIL TEST END ====="
            }
        }

        success {
            echo "✅ Test completed - Check your email!"
        }
    }
}

