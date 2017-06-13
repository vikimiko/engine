/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) since 2016 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.utils;

import io.lumeer.utils.rest.JsonResource;
import io.lumeer.utils.rest.RestRequest;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Imports JSON files through a REST API.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class RestImport {

   @Parameter(names = { "-u", "--base-url"}, description = "Base application URL", required = true)
   private String baseUrl;

   @Parameter(names = { "-H", "--header"}, description = "Additional headers sent to the REST API")
   private List<String> headersParam = new ArrayList<>();

   private MultivaluedMap<String, Object> headers = new  MultivaluedHashMap<>();

   @Parameter(description = "Directory(ies) to search for JSON files", required = true)
   private List<String> rootDirs = new ArrayList<>();

   @Parameter(names = { "--help" }, help = true, description = "Prints out help")
   private boolean help;

   @Parameter(names = { "-v", "--verbose" }, description = "Enables verbose output")
   private boolean verbose;

   private List<JsonResource> includes = new ArrayList<>();

   public static void main(final String[] args) throws Exception {
      final RestImport restImport = new RestImport();
      final JCommander jCommander = new JCommander(restImport);
      jCommander.parse(args);

      if (restImport.baseUrl == null || restImport.rootDirs.isEmpty() || restImport.help) {
         jCommander.usage();
         System.exit(1);
      }

      restImport.parseIncludes();
      restImport.parseHeaders();
      restImport.run();
   }

   private void parseHeaders() {
      headersParam.forEach(s -> {
         if (s.contains("=")) {
            String[] parts = s.split("=", 2);
            headers.add(parts[0], parts[1]);
         }
      });
   }

   private void parseIncludes() throws IOException {
      rootDirs.forEach(rootDir -> {
         System.out.println(rootDir);
         try {
            Files.walkFileTree(Paths.get(rootDir), new SimpleFileVisitor<Path>() {
               @Override
               public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                  if (file.toFile().isFile() && file.toString().endsWith(".json")) {
                     includes.add(new JsonResource(file.toString()));
                  }
                  return FileVisitResult.CONTINUE;
               }
            });
         } catch (IOException e) {
            e.printStackTrace();
         }
      });

      Collections.sort(includes);
   }

   private void run() {
      includes.forEach(this::processFile);
   }

   private void processFile(final JsonResource resource) {
      final JSONParser parser = new JSONParser();

      try {
         final Object o = parser.parse(new FileReader(resource.getPath()));

         if (o instanceof JSONObject) {
            sendRestCommand((JSONObject) o);
         } else {
            throw new IOException("File does not contain JSON object: " + resource.getPath().toString());
         }
      } catch (Exception e) {
         System.err.println("Error processing " + resource.getPath().toString());
         e.printStackTrace();
      }
   }

   private void sendRestCommand(final JSONObject command) {
      final String path = (String) command.get("path");
      final String method = (String) command.get("method");
      final JSONObject commandHeaders = (JSONObject) command.get("headers");
      final Object content = command.get("content");
      RestRequest request = null;

      if (content instanceof JSONObject) {
         request = RestRequest.json(baseUrl, path, method, prepareHeaders(commandHeaders), (JSONObject) content);
      } else if (content instanceof String) {
         request = RestRequest.xml(baseUrl, path, method, prepareHeaders(commandHeaders), (String) content);
      } else if (content instanceof JSONArray) {
         final JSONArray array = (JSONArray) content;
         array.forEach(o -> {
            if (o instanceof JSONObject) {
               final RestRequest r = RestRequest.json(baseUrl, path, method, prepareHeaders(commandHeaders), (JSONObject) o);
               r.invoke();
               r.finalizeResponse(verbose);
            } else if (o instanceof String) {
               final RestRequest r = RestRequest.xml(baseUrl, path, method, prepareHeaders(commandHeaders), (String) o);
               r.invoke();
               r.finalizeResponse(verbose);
            } else {
               System.err.println("Unknown content: " + o.toString());
            }
         });
      } else if (content == null) {
         request = RestRequest.simple(baseUrl, path, method, prepareHeaders(commandHeaders));
      } else {
         System.err.println("Unknown content: " + content.toString());
         return;
      }

      if (request != null) {
         System.out.print(method + " " + path + ": ");
         request.invoke();
         request.finalizeResponse(verbose);
      }
   }

   @SuppressWarnings("unchecked")
   private MultivaluedMap<String, Object> prepareHeaders(final JSONObject commandHeaders) {
      if (commandHeaders == null || commandHeaders.size() == 0) {
         return headers;
      }

      final MultivaluedMap<String, Object> result = new MultivaluedHashMap<>();
      result.putAll(headers);
      commandHeaders.forEach((k, v) -> {
         result.add(k.toString(), v.toString());
      });

      return result;
   }

}
