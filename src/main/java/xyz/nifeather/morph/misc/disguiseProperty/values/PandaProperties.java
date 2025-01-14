package xyz.nifeather.morph.misc.disguiseProperty.values;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.entity.Panda;
import org.bukkit.entity.Panda.Gene;
import org.jetbrains.annotations.Nullable;
import xyz.nifeather.morph.misc.disguiseProperty.SingleProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PandaProperties extends AbstractProperties
{
    private final Map<String, Panda.Gene> geneMap = new ConcurrentHashMap<>();

    private void initMap()
    {
        for (var gene : Gene.values())
            geneMap.put(gene.name().toLowerCase(), gene);
    }

    public final SingleProperty<Panda.Gene> MAIN_GENE = getSingle("panda_main_gene", Gene.NORMAL)
            .withRandom(Gene.values());

    public final SingleProperty<Panda.Gene> HIDDEN_GENE = getSingle("panda_hidden_gene", Gene.NORMAL)
            .withRandom(Gene.values());

    public PandaProperties()
    {
        initMap();

        MAIN_GENE.withValidInput(geneMap.keySet());
        HIDDEN_GENE.withValidInput(geneMap.keySet());

        registerSingle(MAIN_GENE, HIDDEN_GENE);
    }

    @Override
    protected @Nullable Pair<SingleProperty<?>, Object> parseSingleInput(String key, String value)
    {
        switch (key)
        {
            case "panda_main_gene" ->
            {
                var gene = geneMap.getOrDefault(value, null);

                if (gene != null)
                    return Pair.of(MAIN_GENE, gene);
            }

            case "panda_hidden_gene" ->
            {
                var gene = geneMap.getOrDefault(value, null);

                if (gene != null)
                    return Pair.of(HIDDEN_GENE, gene);
            }
        }

        return null;
    }
}
