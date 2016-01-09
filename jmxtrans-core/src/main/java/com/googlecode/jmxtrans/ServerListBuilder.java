/**
 * The MIT License
 * Copyright (c) 2010 JmxTrans team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jmxtrans;

import com.google.common.collect.ImmutableList;
import com.googlecode.jmxtrans.model.OutputWriterFactory;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Server;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

@NotThreadSafe
public class ServerListBuilder {

	@Nonnull private final Map<Server, TemporaryServer> serverMap = newHashMap();
	@Nonnull private final Map<OutputWriterFactory, OutputWriterFactory> outputWriters = newHashMap();

	public void add(Iterable<Server> servers) {
		for (Server server : servers) {
			add(server);
		}
	}

	private void add(Server server) {
		TemporaryServer temporaryServer = findOrCreateTemporaryServer(server);
		temporaryServer.addQueries(server.getQueries());
		temporaryServer.addOutputWriters(server.getOutputWriterFactories());
	}

	private TemporaryServer findOrCreateTemporaryServer(Server server) {
		if (!serverMap.containsKey(server)) {
			serverMap.put(server, new TemporaryServer(server));
		}
		return serverMap.get(server);
	}

	public ImmutableList build() {
		ImmutableList.Builder<Server> builder = ImmutableList.builder();
		for (TemporaryServer server : serverMap.values()) {
			builder.add(server.build());
		}
		return builder.build();
	}

	private OutputWriterFactory singletonOutputWriter(OutputWriterFactory outputWriter) {
		if (!outputWriters.containsKey(outputWriter)) outputWriters.put(outputWriter, outputWriter);
		return outputWriters.get(outputWriter);
	}

	private class TemporaryServer {
		@Nonnull private final Server server;
		@Nonnull private final Map<Query, Set<OutputWriterFactory>> queriesMap = newHashMap();
		@Nonnull private final Set<OutputWriterFactory> temporaryOutputWriters = newHashSet();

		public TemporaryServer(Server server) {
			this.server = server;
		}

		public void addQueries(Iterable<Query> queries) {
			for (Query query : queries) {
				addQuery(query);
			}
		}

		private void addQuery(Query query) {
			if (!queriesMap.containsKey(query)) queriesMap.put(query, new HashSet<OutputWriterFactory>());

			Set<OutputWriterFactory> outputWriters = queriesMap.get(query);
			for (OutputWriterFactory outputWriter : query.getOutputWriters()) {
				outputWriters.add(singletonOutputWriter(outputWriter));
			}
		}

		public void addOutputWriters(Iterable<OutputWriterFactory> outputWriters) {
			for (OutputWriterFactory outputWriter : outputWriters) {
				temporaryOutputWriters.add(singletonOutputWriter(outputWriter));
			}
		}

		public Server build() {
			Server.Builder builder = Server.builder(server)
					.addOutputWriters(outputWriters.values());
			for (Map.Entry<Query, Set<OutputWriterFactory>> queryEntry : queriesMap.entrySet()) {
				builder.addQuery(Query.builder(queryEntry.getKey())
						.addOutputWriters(queryEntry.getValue())
						.build());
			}
			return builder.build();
		}

	}
}
