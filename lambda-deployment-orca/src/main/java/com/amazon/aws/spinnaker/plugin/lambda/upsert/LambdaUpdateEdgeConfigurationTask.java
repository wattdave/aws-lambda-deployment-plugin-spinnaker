/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.amazon.aws.spinnaker.plugin.lambda.upsert;

import com.amazon.aws.spinnaker.plugin.lambda.LambdaCloudOperationOutput;
import com.amazon.aws.spinnaker.plugin.lambda.LambdaStageBaseTask;
import com.amazon.aws.spinnaker.plugin.lambda.upsert.model.LambdaUpdateConfigInput;
import com.amazon.aws.spinnaker.plugin.lambda.upsert.model.LambdaUpdateEdgeConfigInput;
import com.amazon.aws.spinnaker.plugin.lambda.utils.LambdaCloudDriverResponse;
import com.amazon.aws.spinnaker.plugin.lambda.utils.LambdaCloudDriverUtils;
import com.amazon.aws.spinnaker.plugin.lambda.utils.LambdaStageConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.clouddriver.config.CloudDriverConfigurationProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LambdaUpdateEdgeConfigurationTask implements LambdaStageBaseTask {
    private static Logger logger = LoggerFactory.getLogger(LambdaUpdateCodeTask.class);
    private static final ObjectMapper objMapper = new ObjectMapper();
    private static String CLOUDDRIVER_UPSERT_EDGE_CONFIG_PATH = "/aws/ops/updateLambdaEdgeConfiguration";

    @Autowired
    CloudDriverConfigurationProperties props;
    private  String cloudDriverUrl;

    @Autowired
    private LambdaCloudDriverUtils utils;

    @NotNull
    @Override
    public TaskResult execute(@NotNull StageExecution stage) {
        cloudDriverUrl = props.getCloudDriverBaseUrl();
        Boolean edgeEnabled = (Boolean)stage.getContext().getOrDefault(LambdaStageConstants.edgeEnabledKey, Boolean.FALSE);
        if (!edgeEnabled) {
            logger.debug("Nothing to update in lambda Edge");
            return TaskResult.builder(ExecutionStatus.SUCCEEDED).context(stage.getContext()).build();
        }
        LambdaCloudOperationOutput output = this.updateLambdaEdgeConfig(stage);
        Map<String, Object> context = buildContextOutput(output,  LambdaStageConstants.lambdaEdgeConfigurationKey);
        return TaskResult.builder(ExecutionStatus.SUCCEEDED).context(context).build();
    }

    private LambdaCloudOperationOutput updateLambdaEdgeConfig(StageExecution stage) {
        LambdaUpdateEdgeConfigInput inp = utils.getInput(stage, LambdaUpdateEdgeConfigInput.class);
        inp.setAppName(stage.getExecution().getApplication());
        inp.setCredentials(inp.getAccount());
        String rawString = utils.asString(inp);
        String endPoint = cloudDriverUrl + CLOUDDRIVER_UPSERT_EDGE_CONFIG_PATH;
        LambdaCloudDriverResponse respObj = utils.postToCloudDriver(endPoint, rawString);
        String url = cloudDriverUrl + respObj.getResourceUri();
        LambdaCloudOperationOutput operationOutput = LambdaCloudOperationOutput.builder().resourceId(respObj.getId()).url(url).build();
        return operationOutput;
    }

    @Nullable
    @Override
    public TaskResult onTimeout(@NotNull StageExecution stage) {
        return TaskResult.builder(ExecutionStatus.SKIPPED).build();
    }

    @Override
    public void onCancel(@NotNull StageExecution stage) {
    }
}