package com.dongah.fastcharger.utils;

import com.dongah.fastcharger.basefunction.GlobalVariables;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public class SupportFunction {
    private static final Logger logger = LoggerFactory.getLogger(SupportFunction.class);

    public boolean onSupportedFeatureProfiles(String key) {
        boolean result = false;
        try {
            String[] values = getConfigurationValue("SupportedFeatureProfiles").split(",");
            for (String value : values) {
                if (Objects.equals(key, value)) {
                    result = true;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("onSupportedFeatureProfiles error : {}", e.getMessage(), e);
        }
        return result;
    }

    public String getConfigurationValue(String key) {
        String result = "none";
        try {
            FileManagement fileManagement = new FileManagement();
            String configurationString = fileManagement.getStringFromFile(GlobalVariables.getRootPath() + File.separator + "ConfigurationKey");
            JSONObject jsonObjectData = new JSONObject(configurationString);
            JSONArray jsonArrayContent = jsonObjectData.getJSONArray("values");
            for (int i = 0; i < jsonArrayContent.length(); i++) {
                JSONObject contDetail = jsonArrayContent.getJSONObject(i);
                if (Objects.equals(contDetail.get("key"), key)) {
                    result = contDetail.getString("value");
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("getConfigurationValue error : {}", e.getMessage(), e);
        }
        return result;
    }
}
