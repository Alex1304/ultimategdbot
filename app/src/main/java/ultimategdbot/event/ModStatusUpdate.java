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
        DEMOTED_FROM_MOD,
        DEMOTED_FROM_ELDER;

        public EmbedType embedType() {
            if (name().startsWith("PROMOTED")) {
                return EmbedType.MOD;
            } else if (name().startsWith("DEMOTED")) {
                return EmbedType.UNMOD;
            } else {
                throw new AssertionError();
            }
        }

        public List<String> selectList(UltimateGDBotConfig.GD.Events.RandomMessages randomMessages) {
            switch (this) {
                case PROMOTED_TO_MOD:
                    return randomMessages.mod();
                case PROMOTED_TO_ELDER:
                    return randomMessages.elderMod();
                case DEMOTED_FROM_MOD:
                    return randomMessages.unmod();
                case DEMOTED_FROM_ELDER:
                    return randomMessages.elderUnmod();
                default:
                    throw new AssertionError();
            }
        }
    }
}
