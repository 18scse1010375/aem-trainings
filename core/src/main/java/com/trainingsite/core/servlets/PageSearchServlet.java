package com.trainingsite.core.servlets;

import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Iterator;

@Component(service = { Servlet.class },
        property = {
                "sling.servlet.paths=/bin/search/pages",
                "sling.servlet.methods=GET"
        })
public class PageSearchServlet extends SlingAllMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String title = request.getParameter("searchTerm");
        ObjectMapper mapper = new ObjectMapper();
        ResourceResolver resourceResolver = request.getResourceResolver();
        JSONArray pageVals = null;

        try {
            pageVals = getAllPagesByTitleAsJson(resourceResolver, title);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonString;
        if (pageVals != null && pageVals.length() > 0) {
            jsonString = mapper.writeValueAsString(pageVals);
        } else {
            // Return an empty JSON array if no pages are found
            jsonString = "[]"; //NO DATA FOUND
        }

        // Write the JSON response
        response.getWriter().write(pageVals.toString());
    }

    public JSONArray getAllPagesByTitleAsJson(ResourceResolver resourceResolver, String title) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        Resource resource = resourceResolver.getResource("/content/trainingsite/us/en");

        if (resource != null) {
            Page page = resource.adaptTo(Page.class);
            if (page != null) {
                Iterator<Page> pageIterator = page.listChildren();
                while (pageIterator.hasNext()) {
                    Page nextPage = pageIterator.next();
                    System.out.println("Checking Page: " + nextPage.getTitle()); // Log each page title

                    // Check if the page title matches the provided title
                    if (nextPage.getTitle() != null && nextPage.getTitle().equalsIgnoreCase(title)) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("path", nextPage.getPath());
                        jsonObject.put("title", nextPage.getTitle());
                        jsonObject.put("name", nextPage.getName());
                        jsonObject.put("description", getPageDescription(nextPage));
                        jsonObject.put("pageTitle", nextPage.getPageTitle());
                        jsonArray.put(jsonObject);
                    }
                }
            }
        }

        return jsonArray;
    }

    private String getPageDescription(Page page) {
        // Retrieve the description from jcr:content
        Resource contentResource = page.getContentResource();
        if (contentResource != null) {
            ValueMap properties = contentResource.adaptTo(ValueMap.class);
            return properties != null ? properties.get("jcr:description", String.class) : ""; // Return empty string if description is not found
        }
        return ""; // Return empty string if content resource is null
    }
}