/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.action.search.template;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.ElasticsearchClient;

public class SearchTemplateAction extends Action<SearchTemplateRequest, SearchTemplateResponse, SearchTemplateRequestBuilder> {

    public static final SearchTemplateAction INSTANCE = new SearchTemplateAction();
    public static final String NAME = "indices:data/read/search/template";

    private SearchTemplateAction() {
        super(NAME);
    }

    @Override
    public SearchTemplateRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new SearchTemplateRequestBuilder(client, this);
    }

    @Override
    public SearchTemplateResponse newResponse() {
        return new SearchTemplateResponse();
    }
}
