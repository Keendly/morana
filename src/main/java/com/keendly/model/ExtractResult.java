/*
 *  Copyright 2011 Peter Karich 
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.keendly.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ExtractResult implements Serializable {

    public String title;
    public String url;
    public String originalUrl;
    public String canonicalUrl;
    public String imageUrl;
    public String videoUrl;
    public String rssUrl;
    public String text;
    public String faviconUrl;
    public String description;
    public String authorName;
    public String authorDescription;
    public Date date;
    public Collection<String> keywords;
    public List<ImageResult> images = null;
    public List<Map<String,String>> links = new ArrayList<>();
    public String type;
    public String siteName;
    public String language;
}
