package fr.segame.armesiaMenu.condition;

import java.util.Map;

public class ConditionParser {

    public static Condition parse(Map<String, Object> map) {

        String type = (String) map.get("type");

        switch (type) {
            case "money":
                return new MoneyCondition(((Number) map.get("min")).doubleValue());

            case "level":
                return new LevelCondition(((Number) map.get("min")).intValue());
        }

        return null;
    }
}