package io.github.james5635.chafgram.social;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps `graphql/schema.graphqls` and the DTO records in `social.dto` in lockstep:
 * every object type in the schema must exist as a `<Type>Dto` record with exactly
 * the same component names, and every enum value must exist as a Java constant.
 *
 * If you edit one side without the other, this test fails.
 */
class SchemaDtoSyncTest {

	private final String schema = readSchema();

	private static String readSchema() {
		// surefire's working directory is the module basedir, so the real files are readable
		Path dir = Paths.get("src/main/resources/graphql");
		StringBuilder all = new StringBuilder();
		try (Stream<Path> stream = Files.list(dir)) {
			for (Path file : stream.filter(p -> p.toString().endsWith(".graphqls")).toList()) {
				all.append(Files.readString(file)).append('\n');
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Cannot read schema files from " + dir, e);
		}
		return all.toString();
	}

	@Test
	void everySchemaObjectTypeHasMatchingDto() {
		Map<String, Set<String>> schemaTypes = objectTypes(schema);
		Map<String, Set<String>> dtos = new HashMap<>();
		for (String type : schemaTypes.keySet()) {
			String pkg = TYPE_TO_PACKAGE.get(type);
			assertThat(pkg)
					.as("GraphQL type %s has no DTO package registered in this test", type)
					.isNotNull();
			Set<String> components = recordComponents(pkg + "." + type + "Dto");
			dtos.put(type, components);
		}
		for (var entry : schemaTypes.entrySet()) {
			assertThat(dtos.get(entry.getKey()))
					.as("DTO %sDto must have exactly the same components as schema type %s",
							entry.getKey(), entry.getKey())
					.containsExactlyInAnyOrderElementsOf(entry.getValue());
		}
	}

	@Test
	void everySchemaEnumMatchesItsJavaEnum() {
		Map<String, String> enumClasses = Map.of(
				"PostType", "io.github.james5635.chafgram.social.PostType",
				"ReactionType", "io.github.james5635.chafgram.social.ReactionType",
				"NotificationType", "io.github.james5635.chafgram.notifications.NotificationType");
		for (var entry : enumTypes(schema).entrySet()) {
			String className = enumClasses.get(entry.getKey());
			assertThat(className).as("Schema enum %s has no Java counterpart registered in this test", entry.getKey())
					.isNotNull();
			Set<String> javaValues = javaEnumConstants(className);
			assertThat(javaValues)
					.as("Enum %s must have the same constants as the schema enum", entry.getKey())
					.containsExactlyInAnyOrderElementsOf(entry.getValue());
		}
	}

	/** GraphQL type name → package that owns the matching <Name>Dto class */
	private static final Map<String, String> TYPE_TO_PACKAGE = Map.ofEntries(
			Map.entry("Member", "io.github.james5635.chafgram.social.dto"),
			Map.entry("Post", "io.github.james5635.chafgram.social.dto"),
			Map.entry("Comment", "io.github.james5635.chafgram.social.dto"),
			Map.entry("Notification", "io.github.james5635.chafgram.social.dto"),
			Map.entry("ReactionSummary", "io.github.james5635.chafgram.social.dto"),
			Map.entry("FeedPage", "io.github.james5635.chafgram.social.dto"),
			Map.entry("Workspace", "io.github.james5635.chafgram.identity.dto"),
			Map.entry("Invite", "io.github.james5635.chafgram.identity.dto"),
			Map.entry("CreateInvite", "io.github.james5635.chafgram.identity.dto"),
			Map.entry("MemberDetail", "io.github.james5635.chafgram.identity.dto"),
			Map.entry("Passkey", "io.github.james5635.chafgram.identity.dto"),
			Map.entry("MediaAsset", "io.github.james5635.chafgram.media.dto"),
			Map.entry("Conversation", "io.github.james5635.chafgram.messaging.dto"),
			Map.entry("Message", "io.github.james5635.chafgram.messaging.dto"),
			Map.entry("Report", "io.github.james5635.chafgram.moderation.dto"),
			Map.entry("AuditLogEntry", "io.github.james5635.chafgram.moderation.dto"));

	/** type Name { field: Type ... }, excluding Query/Mutation */
	private static Map<String, Set<String>> objectTypes(String schema) {
		Map<String, Set<String>> out = new HashMap<>();
		Matcher m = Pattern.compile("type\\s+(\\w+)\\s*\\{([^}]*)\\}").matcher(schema);
		while (m.find()) {
			String name = m.group(1);
			if (name.equals("Query") || name.equals("Mutation") || name.equals("schema")) {
				continue;
			}
			out.put(name, fields(m.group(2)));
		}
		return out;
	}

	private static Map<String, Set<String>> enumTypes(String schema) {
		Map<String, Set<String>> out = new HashMap<>();
		Matcher m = Pattern.compile("enum\\s+(\\w+)\\s*\\{([^}]*)\\}").matcher(schema);
		while (m.find()) {
			Set<String> values = Arrays.stream(m.group(2).trim().split("\\s+"))
					.filter(v -> !v.isEmpty() && !v.startsWith("#"))
					.collect(Collectors.toCollection(LinkedHashSet::new));
			out.put(m.group(1), values);
		}
		return out;
	}

	/** field names only, ignoring arguments, directives and list wrappers */
	private static Set<String> fields(String body) {
		return Arrays.stream(body.split("\\r?\\n"))
				.map(String::trim)
				.filter(line -> !line.isEmpty() && !line.startsWith("#"))
				.map(line -> {
					int colon = line.indexOf(':');
					return colon < 0 ? "" : line.substring(0, colon).trim();
				})
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static Set<String> recordComponents(String className) {
		try {
			Class<?> dto = Class.forName(className);
			assertThat(dto.isRecord()).as("%s must be a record", className).isTrue();
			return Arrays.stream(dto.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}
		catch (ClassNotFoundException e) {
			throw new AssertionError("Missing DTO class: " + className, e);
		}
	}

	private static Set<String> javaEnumConstants(String className) {
		try {
			Enum<?>[] constants = (Enum<?>[]) Class.forName(className).getEnumConstants();
			return Arrays.stream(constants)
					.map(Enum::name)
					.collect(Collectors.toCollection(LinkedHashSet::new));
		}
		catch (ClassNotFoundException e) {
			throw new AssertionError("Missing enum class: " + className, e);
		}
	}
}
