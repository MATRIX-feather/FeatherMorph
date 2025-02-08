package xyz.nifeather.morph.backends.server.renderer.network;

import com.comphenix.protocol.wrappers.WrappedDataValue;
import xyz.nifeather.morph.backends.server.renderer.network.datawatcher.values.SingleValue;

/**
 * 自定义序列化方法
 * @param <X> 要序列化的对象类型
 */
public interface ICustomSerializeMethod<X>
{
    WrappedDataValue apply(SingleValue<X> value, X valueObj);
}
