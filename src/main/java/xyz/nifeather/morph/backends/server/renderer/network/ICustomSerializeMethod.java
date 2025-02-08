package xyz.nifeather.morph.backends.server.renderer.network;

import com.comphenix.protocol.wrappers.WrappedDataValue;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.SingleValue;

public interface ICustomSerializeMethod<X>
{
    WrappedDataValue apply(SingleValue<X> value, X valueObj);
}
