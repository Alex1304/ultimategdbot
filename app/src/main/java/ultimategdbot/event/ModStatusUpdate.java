package ultimategdbot.event;

import jdash.common.entity.GDUserProfile;
import org.immutables.value.Value;
import ultimategdbot.config.UltimateGDBotConfig;
import ultimategdbot.util.EmbedType;

import java.util.List;

@Value.Immutable
public interface ModStatusUpdate {

    @Value.Parameter
    GDUserProfile user();

    @Value.Parameter
    Type type();

    enum Type {
        PROMOTED_TO_MOD,
        PROMOTED_TO_ELDER,
        PROMOTED_TO_LBMOD,
        DEMOTED_FROM_MOD,
        DEMOTED_FROM_ELDER,
        DEMOTED_FROM_LBMOD;

        public EmbedType embedType() {
            if (name().startsWith("PROMOTED")) {
                return EmbedType.MOD;
            } else {
                return EmbedType.UNMOD;
            }
        }

        public List<String> selectList(UltimateGDBotConfig.GD.Events.RandomMessages randomMessages) {
            return switch (this) {
                case PROMOTED_TO_MOD -> randomMessages.mod();
                case PROMOTED_TO_ELDER -> randomMessages.elderMod();
                case PROMOTED_TO_LBMOD -> randomMessages.lbMod();
                case DEMOTED_FROM_MOD -> randomMessages.unmod();
                case DEMOTED_FROM_ELDER -> randomMessages.elderUnmod();
                case DEMOTED_FROM_LBMOD -> randomMessages.lbUnmod();
            };
        }
    }
}
