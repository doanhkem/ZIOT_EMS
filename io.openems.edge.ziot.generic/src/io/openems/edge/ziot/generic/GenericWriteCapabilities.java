package io.openems.edge.ziot.generic;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.openems.edge.common.channel.ChannelId;

final class GenericWriteCapabilities {

	private final Set<String> mappedChannelIds;

	private GenericWriteCapabilities(Set<String> mappedChannelIds) {
		this.mappedChannelIds = mappedChannelIds;
	}

	static GenericWriteCapabilities of(GenericMapping mapping, Map<String, ChannelId> channels) {
		return new GenericWriteCapabilities(mapping.writeRegisters.stream() //
				.filter(GenericMapping.Register::isMapped) //
				.map(register -> channels.get(register.tagName)) //
				.filter(channelId -> channelId != null) //
				.map(ChannelId::id) //
				.collect(Collectors.toUnmodifiableSet()));
	}

	boolean has(ChannelId channelId) {
		return this.mappedChannelIds.contains(channelId.id());
	}
}
