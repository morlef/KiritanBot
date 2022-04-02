package si.f5.luna3419.krtn;

import discord4j.core.object.entity.User;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Pattern pattern = Pattern.compile("\\$\\{.*}");

    public static long longOrDefault(Long l, long def) {
        return Objects.requireNonNullElse(l, def);
    }

    public static String s(User user, String text, @Nullable String[] args) {
        text = replaceIfContains(text, "${user}", user.getUsername());

        Holder<String> textHolder = new Holder<>();
        textHolder.set(text);

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                textHolder.set(replaceIfContains(textHolder.get(), "${" + i + "}", args[i]));
            }
        }

        Matcher matcher = pattern.matcher(textHolder.get());

        while (matcher.find()) {
            Bot.getInstance().getConfig().getCommands().forEach((key, value) -> textHolder.set(replaceIfContains(textHolder.get(), "${commands." + key + "}", value)));
            Bot.getInstance().getConfig().getMessages().forEach((key, value) -> textHolder.set(replaceIfContains(textHolder.get(), "${messages." + key + "}", value)));

            matcher = pattern.matcher(textHolder.get());
        }
        return textHolder.get();
    }

    public static String replaceIfContains(String text, String s, String replace) {
        return text.contains(s) ? text.replace(s, replace) : text;
    }

    public static String[] splitWithSpaces(String str) {
        return str.replace("　", " ").split(" ");
    }

    public static class Holder<T> {
        private T obj;

        public void set(T obj) {
            this.obj = obj;
        }

        public T get() {
            return obj;
        }
    }
}
