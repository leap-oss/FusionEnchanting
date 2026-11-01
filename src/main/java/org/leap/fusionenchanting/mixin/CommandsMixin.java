package org.leap.fusionenchanting.mixin;
import org.leap.fusionenchanting.command.FusionCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;
    @Inject(method="<init>",at=@At("RETURN"))
    private void fusion$commands(CallbackInfo ci){FusionCommand.register(dispatcher);}
}
