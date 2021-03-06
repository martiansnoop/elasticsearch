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
package org.elasticsearch.search.aggregations.bucket.significant;

import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.aggregations.AggregationStreams;
import org.elasticsearch.search.aggregations.InternalAggregation;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.aggregations.bucket.significant.heuristics.SignificanceHeuristic;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class SignificantStringTerms extends InternalSignificantTerms<SignificantStringTerms, SignificantStringTerms.Bucket> {

    public static final InternalAggregation.Type TYPE = new Type("significant_terms", "sigsterms");

    public static final AggregationStreams.Stream STREAM = new AggregationStreams.Stream() {
        @Override
        public SignificantStringTerms readResult(StreamInput in) throws IOException {
            SignificantStringTerms buckets = new SignificantStringTerms();
            buckets.readFrom(in);
            return buckets;
        }
    };

    public static void registerStream() {
        AggregationStreams.registerStream(STREAM, TYPE.stream());
    }

    public static void registerStreams() {
        AggregationStreams.registerStream(STREAM, TYPE.stream());
    }

    public static class Bucket extends InternalSignificantTerms.Bucket {

        BytesRef termBytes;

        public Bucket(long subsetSize, long supersetSize, DocValueFormat format) {
            // for serialization
            super(subsetSize, supersetSize, format);
        }

        public Bucket(BytesRef term, long subsetDf, long subsetSize, long supersetDf, long supersetSize, InternalAggregations aggregations,
                DocValueFormat format) {
            super(subsetDf, subsetSize, supersetDf, supersetSize, aggregations, format);
            this.termBytes = term;
        }

        public Bucket(BytesRef term, long subsetDf, long subsetSize, long supersetDf, long supersetSize,
                InternalAggregations aggregations, double score, DocValueFormat format) {
            this(term, subsetDf, subsetSize, supersetDf, supersetSize, aggregations, format);
            this.score = score;
        }

        @Override
        public Number getKeyAsNumber() {
            // this method is needed for scripted numeric aggregations
            return Double.parseDouble(termBytes.utf8ToString());
        }

        @Override
        int compareTerm(SignificantTerms.Bucket other) {
            return termBytes.compareTo(((Bucket) other).termBytes);
        }

        @Override
        public String getKeyAsString() {
            return format.format(termBytes);
        }

        @Override
        public String getKey() {
            return getKeyAsString();
        }

        @Override
        Bucket newBucket(long subsetDf, long subsetSize, long supersetDf, long supersetSize, InternalAggregations aggregations) {
            return new Bucket(termBytes, subsetDf, subsetSize, supersetDf, supersetSize, aggregations, format);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            termBytes = in.readBytesRef();
            subsetDf = in.readVLong();
            supersetDf = in.readVLong();
            score = in.readDouble();
            aggregations = InternalAggregations.readAggregations(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeBytesRef(termBytes);
            out.writeVLong(subsetDf);
            out.writeVLong(supersetDf);
            out.writeDouble(getSignificanceScore());
            aggregations.writeTo(out);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(CommonFields.KEY, getKeyAsString());
            builder.field(CommonFields.DOC_COUNT, getDocCount());
            builder.field("score", score);
            builder.field("bg_count", supersetDf);
            aggregations.toXContentInternal(builder, params);
            builder.endObject();
            return builder;
        }
    }

    SignificantStringTerms() {} // for serialization

    public SignificantStringTerms(long subsetSize, long supersetSize, String name, DocValueFormat format, int requiredSize,
            long minDocCount, SignificanceHeuristic significanceHeuristic, List<? extends InternalSignificantTerms.Bucket> buckets,
            List<PipelineAggregator> pipelineAggregators,
            Map<String, Object> metaData) {
        super(subsetSize, supersetSize, name, format, requiredSize, minDocCount, significanceHeuristic, buckets, pipelineAggregators, metaData);
    }

    @Override
    public Type type() {
        return TYPE;
    }

    @Override
    public SignificantStringTerms create(List<SignificantStringTerms.Bucket> buckets) {
        return new SignificantStringTerms(this.subsetSize, this.supersetSize, this.name, this.format, this.requiredSize, this.minDocCount,
                this.significanceHeuristic, buckets, this.pipelineAggregators(), this.metaData);
    }

    @Override
    public Bucket createBucket(InternalAggregations aggregations, SignificantStringTerms.Bucket prototype) {
        return new Bucket(prototype.termBytes, prototype.subsetDf, prototype.subsetSize, prototype.supersetDf, prototype.supersetSize,
                aggregations, prototype.format);
    }

    @Override
    protected SignificantStringTerms create(long subsetSize, long supersetSize, List<InternalSignificantTerms.Bucket> buckets,
            InternalSignificantTerms prototype) {
        return new SignificantStringTerms(subsetSize, supersetSize, prototype.getName(), prototype.format, prototype.requiredSize,
                prototype.minDocCount, prototype.significanceHeuristic, buckets, prototype.pipelineAggregators(), prototype.getMetaData());
    }

    @Override
    protected void doReadFrom(StreamInput in) throws IOException {
        this.format = in.readNamedWriteable(DocValueFormat.class);
        this.requiredSize = readSize(in);
        this.minDocCount = in.readVLong();
        this.subsetSize = in.readVLong();
        this.supersetSize = in.readVLong();
        significanceHeuristic = in.readNamedWriteable(SignificanceHeuristic.class);
        int size = in.readVInt();
        List<InternalSignificantTerms.Bucket> buckets = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Bucket bucket = new Bucket(subsetSize, supersetSize, format);
            bucket.readFrom(in);
            buckets.add(bucket);
        }
        this.buckets = buckets;
        this.bucketMap = null;
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        out.writeNamedWriteable(format);
        writeSize(requiredSize, out);
        out.writeVLong(minDocCount);
        out.writeVLong(subsetSize);
        out.writeVLong(supersetSize);
        out.writeNamedWriteable(significanceHeuristic);
        out.writeVInt(buckets.size());
        for (InternalSignificantTerms.Bucket bucket : buckets) {
            bucket.writeTo(out);
        }
    }

    @Override
    public XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
        builder.field("doc_count", subsetSize);
        builder.startArray(CommonFields.BUCKETS);
        for (InternalSignificantTerms.Bucket bucket : buckets) {
            //There is a condition (presumably when only one shard has a bucket?) where reduce is not called
            // and I end up with buckets that contravene the user's min_doc_count criteria in my reducer
            if (bucket.subsetDf >= minDocCount) {
                bucket.toXContent(builder, params);
            }
        }
        builder.endArray();
        return builder;
    }

}
