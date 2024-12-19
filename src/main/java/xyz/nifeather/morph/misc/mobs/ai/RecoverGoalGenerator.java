package xyz.nifeather.morph.misc.mobs.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import xiamomc.pluginbase.Exceptions.NullDependencyException;
import xyz.nifeather.morph.MorphPlugin;

import java.lang.reflect.Constructor;

public class RecoverGoalGenerator
{
    public static  <T extends PathfinderMob> AvoidEntityGoal<Player> generateRecover(Class<T> entityClazz, T entity, String className,
                                                                                     double detectDistance, double walkSpeed, double sprintSpeed)
    {
        var log = MorphPlugin.getInstance().getSLF4JLogger();
        var declaredClasses = Cat.class.getDeclaredClasses();

        for (Class<?> declaredClass : declaredClasses)
        {
            var name = declaredClass.getSimpleName();
            //log.info("Find class with simple name '%s'".formatted(name));

            if (!name.equals(className)) continue;

            Constructor<?> constructor = null;

            try
            {
                // Ocelot ocelot, Class<T> fleeFromType, float distance, double slowSpeed, double fastSpeed
                constructor = declaredClass.getDeclaredConstructor(entityClazz, Class.class, float.class, double.class, double.class);
                constructor.setAccessible(true);
                Object instance = constructor.newInstance(entity, Player.class, detectDistance, walkSpeed, sprintSpeed);

                if (!(instance instanceof AvoidEntityGoal<?> avoidEntityGoal))
                    throw new NullDependencyException("Created instance not an AvoidEntityGoal?!");

                return (AvoidEntityGoal<Player>) avoidEntityGoal;
            }
            catch (Throwable t)
            {
                log.warn("Unable to create '%s: %s".formatted(className, t.getMessage()));
                log.warn("Using common AvoidEntityGoal...");
                t.printStackTrace();
            }

            if (constructor == null)
                return null;
        }

        return null;
    }
}
