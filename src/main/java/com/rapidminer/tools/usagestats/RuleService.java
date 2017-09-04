/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools.usagestats;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidminer.RapidMiner;
import com.rapidminer.RapidMiner.ExecutionMode;
import com.rapidminer.studio.internal.RuleProvider;
import com.rapidminer.studio.internal.RuleProviderRegistry;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;


/**
 * This class loads the CTA rules either from a local test file or from Nexus.
 *
 * @author Jonas Wilms-Pfau, Marco Boeck
 * @since 7.5
 *
 */
enum RuleService {

	INSTANCE;

	private final Set<String> PROHIBITED_KEYWORDS = new HashSet<>();

	private Set<VerifiableRule> rules = new HashSet<>();

	private RuleService() {
		PROHIBITED_KEYWORDS.add("JOIN");
		PROHIBITED_KEYWORDS.add("CREATE");
		PROHIBITED_KEYWORDS.add("INSERT");
		PROHIBITED_KEYWORDS.add("UPDATE");
		PROHIBITED_KEYWORDS.add("DELETE");
		PROHIBITED_KEYWORDS.add("DROP");
		PROHIBITED_KEYWORDS.add("ALTER");
		PROHIBITED_KEYWORDS.add("MERGE");
		PROHIBITED_KEYWORDS.add("TRUNCATE");
		PROHIBITED_KEYWORDS.add("SET");
		PROHIBITED_KEYWORDS.add("SHUTDOWN");
		PROHIBITED_KEYWORDS.add("COMMIT");
		PROHIBITED_KEYWORDS.add("GRANT");
		PROHIBITED_KEYWORDS.add("CHECKPOINT");
		PROHIBITED_KEYWORDS.add("SAVEPOINT");
		PROHIBITED_KEYWORDS.add("PREPARE");
		PROHIBITED_KEYWORDS.add("REVOKE");
		PROHIBITED_KEYWORDS.add("ROLLBACK");
		PROHIBITED_KEYWORDS.add("CONSTRAINT");
		PROHIBITED_KEYWORDS.add("RUNSCRIPT");
		PROHIBITED_KEYWORDS.add("BACKUP");
		PROHIBITED_KEYWORDS.add("CALL");
		PROHIBITED_KEYWORDS.add("SCRIPT");
		PROHIBITED_KEYWORDS.add("ANALYZE");
		PROHIBITED_KEYWORDS.add("COMMENT");
		PROHIBITED_KEYWORDS.add("EXPLAIN");
		PROHIBITED_KEYWORDS.add("SHOW");

		reloadRules();
	}

	public Set<VerifiableRule> getRules() {
		return rules;
	}

	/**
	 * Reloads the CTA rules from either local file or Nexus.
	 */
	public void reloadRules() {
		if (!RapidMiner.getExecutionMode().equals(ExecutionMode.UI)) {
			return;
		}

		ObjectMapper mapper = new ObjectMapper();
		List<VerifiableRule> newRules = null;
		Iterator<RuleProvider> ruleProvider = RuleProviderRegistry.INSTANCE.getRuleProvider().iterator();

		while (ruleProvider.hasNext() && newRules == null) {
			RuleProvider provider = ruleProvider.next();
			try (InputStream ruleJson = provider.getRuleJson()) {
				if (ruleJson != null) {
					newRules = checkAndConvertRules(mapper.readValue(ruleJson, new TypeReference<List<Rule>>() {
					}));
				} else {
					LogService.getRoot().log(Level.FINE, I18N.getMessage(LogService.getRoot().getResourceBundle(),
							"com.rapidminer.tools.usagestats.RuleService.load.empty", provider.getClass().getSimpleName()));
				}
			} catch (Exception e) {
				LogService.getRoot().log(Level.WARNING, I18N.getMessage(LogService.getRoot().getResourceBundle(),
						"com.rapidminer.tools.usagestats.RuleService.load.failure", provider.getClass().getSimpleName()), e);
				break;
			}
		}

		if (newRules != null) {
			rules.retainAll(newRules);
			rules.addAll(newRules);
		}

	}

	/**
	 * Converts JSON rules list to a {@link VerifiableRule} list. Also checks that rules do not
	 * violate the {@link #PROHIBITED_KEYWORDS} SQL blacklist. If any rules does, it is skipped.
	 *
	 * @param jsonRules
	 *            the input rule list
	 * @return the output verifiable rule list
	 */
	private List<VerifiableRule> checkAndConvertRules(List<Rule> jsonRules) {
		List<VerifiableRule> newRules = new ArrayList<>(jsonRules.size());
		ruleLoop: for (Rule rule : jsonRules) {
			for (String sql : rule.getQueries()) {
				for (String prohibited : PROHIBITED_KEYWORDS) {
					if (sql.toUpperCase(Locale.ENGLISH).contains(prohibited)) {
						// prohibited keyword found, skip this rule
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.tools.usagestats.RuleService.load_invalid_sql",
								new Object[] { rule.getId(), prohibited });
						continue ruleLoop;
					}
				}
			}
			newRules.add(new VerifiableRule(rule));
		}

		return newRules;
	}

}
