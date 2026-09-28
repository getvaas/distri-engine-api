/*
 * CI Pipeline for distri-engine-api
 *
 * Uses vaas-shared library for common pipeline logic.
 * See: https://github.com/getvaas/vaas-jenkins-shared-pipeline
 */

@Library('vaas-shared') _

ciPipeline(
    project: 'distri-engine',
    slackChannel: 'elisir-team',
    nextCdJob: 'CD-distri-engine',
    terraformFolder: 'deploy/terraform',
    terraformConfigBaseFolder: 'configuration',
    testDockerfile: 'DockerfileTest',
    testCommand: 'clean test',
    testOutputPath: '/home/app/build/reports/tests/test/index.html',
    testOutputFile: './tests.html'
)
