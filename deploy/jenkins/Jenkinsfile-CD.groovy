/*
 * CD Pipeline for distri-engine-api
 *
 * Uses vaas-shared library for common pipeline logic.
 * See: https://github.com/getvaas/vaas-jenkins-shared-pipeline
 */

@Library('vaas-shared') _

cdPipeline(
    project: 'distri-engine',
    slackChannel: 'elisir-team',
    ecsServiceName: 'distri-engine-api',
    taskDefinitionName: 'distri-engine-api',
    ecrRepositoryName: 'distri-engine-api-repository',
    s3BucketPath: 'distri-engine-api',
    terraformFolder: 'deploy/terraform',
    terraformConfigBaseFolder: 'configuration'
)
