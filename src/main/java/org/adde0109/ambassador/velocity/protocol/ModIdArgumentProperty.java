package org.adde0109.ambassador.velocity.protocol;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.ModInfo;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ModIdArgumentProperty implements ArgumentType<String> {

  public ModIdArgumentProperty() {}

  @Override
  public String parse(StringReader reader) throws CommandSyntaxException {
    return reader.readUnquotedString();
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context,
                                                            SuggestionsBuilder builder) {
    S source = context.getSource();

    if (source instanceof Player) {
      ModInfo modInfo = ((Player) source).getModInfo().orElse(null);

      if (modInfo != null) {
        for (ModInfo.Mod mod : modInfo.getMods()) {
          builder.suggest(mod.getId());
        }

        return builder.buildFuture();
      }
    }

    return Suggestions.empty();
  }

  @Override
  public Collection<String> getExamples() {
    throw new UnsupportedOperationException();
  }
}