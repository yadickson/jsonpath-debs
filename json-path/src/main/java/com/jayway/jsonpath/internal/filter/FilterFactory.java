/*
 * Copyright 2011 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.jsonpath.internal.filter;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.internal.PathToken;

/**
 * @author Kalle Stenflo
 */
public class FilterFactory {

    private final static PathTokenFilter DOCUMENT_FILTER = new PassthroughFilter("$", false);
    private final static PathTokenFilter ALL_ARRAY_ITEMS_FILTER = new PassthroughFilter("[*]", true);
    private final static PathTokenFilter WILDCARD_FILTER = new WildcardFilter("*");
    private final static PathTokenFilter SCAN_FILTER = new ScanFilter("..");
    private final static PathTokenFilter ARRAY_QUERY_FILTER = new ArrayQueryFilter("[?]");

    public static PathTokenFilter createFilter(PathToken token) {

        String pathFragment = token.getFragment();

        if (DOCUMENT_FILTER.getCondition().equals(pathFragment) && token.isRootToken()) {     //"$"

            return DOCUMENT_FILTER;

        } else if (ALL_ARRAY_ITEMS_FILTER.getCondition().equals(pathFragment)) {   //"[*]"

            return ALL_ARRAY_ITEMS_FILTER;

        } else if ("*".equals(pathFragment)) {

            return WILDCARD_FILTER;

        } else if (SCAN_FILTER.getCondition().equals(pathFragment)) {

            return SCAN_FILTER;

        } else if (ARRAY_QUERY_FILTER.getCondition().equals(pathFragment)) { //"[?]"

            return ARRAY_QUERY_FILTER;

        } else if (!pathFragment.contains("[")) {

            return new FieldFilter(token);

        } else if (pathFragment.contains("[")) {

            if (pathFragment.startsWith("[?")) {

                if(ArrayEvalFilter.isConditionStatement(pathFragment)){
                    return new ArrayEvalFilter(pathFragment);
                } else if (!pathFragment.contains("=") && !pathFragment.contains("<") && !pathFragment.contains(">")) {
                    //[?(@.isbn)]
                    return new HasPathFilter(pathFragment);
                } else {
                    throw new InvalidPathException("Failed to create PathTokenFilter for path fragment: " + pathFragment);
                }
            } else {
                //[0]
                //[0,1, ...]
                //[-1:]
                //[:1]
                return new ArrayIndexFilter(pathFragment);
            }
        }

        throw new UnsupportedOperationException("can not find filter for path fragment " + pathFragment);

    }


}
