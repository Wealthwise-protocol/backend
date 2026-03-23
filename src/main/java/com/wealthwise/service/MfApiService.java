package com.wealthwise.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class MfApiService {

    private static final String BASE_URL = "https://api.mfapi.in/mf";
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getFundDetails(String schemeCode) {
        try {
            String url = BASE_URL + "/" + schemeCode;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return null;
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", schemeCode);
            
            Map<String, Object> meta = (Map<String, Object>) response.get("meta");
            if (meta != null) {
                result.put("name", meta.get("scheme_name"));
                result.put("amc", meta.get("fund_house"));
                result.put("category", meta.get("scheme_category"));
                result.put("subcategory", meta.get("scheme_type"));
            }
            
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (data != null && !data.isEmpty()) {
                Map<String, Object> latest = data.get(0);
                result.put("nav", new BigDecimal(latest.get("nav").toString()));
                result.put("date", latest.get("date"));
            }
            
            List<Map<String, Object>> navHistory = new ArrayList<>();
            if (data != null) {
                for (Map<String, Object> node : data) {
                    Map<String, Object> navPoint = new HashMap<>();
                    navPoint.put("date", node.get("date"));
                    navPoint.put("nav", new BigDecimal(node.get("nav").toString()));
                    navHistory.add(navPoint);
                }
            }
            result.put("navHistory", navHistory);
            
            return result;
        } catch (Exception e) {
            log.error("Error fetching fund details for scheme: {}", schemeCode, e);
            return null;
        }
    }

    public Map<String, Object> getLatestNav(String schemeCode) {
        try {
            String url = BASE_URL + "/" + schemeCode + "/latest";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return null;
            
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
            if (data != null && !data.isEmpty()) {
                Map<String, Object> latest = data.get(0);
                Map<String, Object> result = new HashMap<>();
                result.put("nav", new BigDecimal(latest.get("nav").toString()));
                result.put("date", latest.get("date"));
                return result;
            }
            return null;
        } catch (Exception e) {
            log.error("Error fetching latest NAV for scheme: {}", schemeCode, e);
            return null;
        }
    }

    public List<Map<String, Object>> searchFunds(String query) {
        try {
            String url = BASE_URL + "/search?q=" + query;
            List<Map<String, Object>> response = restTemplate.getForObject(url, List.class);
            if (response == null) return Collections.emptyList();
            
            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<String, Object> node : response) {
                Map<String, Object> fund = new HashMap<>();
                fund.put("id", node.get("schemeCode").toString());
                fund.put("name", node.get("schemeName"));
                results.add(fund);
            }
            return results;
        } catch (Exception e) {
            log.error("Error searching funds with query: {}", query, e);
            return Collections.emptyList();
        }
    }
}
