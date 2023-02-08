/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.adde0109.ambassador.forgeCommandArgument;

import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.DefinedPacket;


import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * An argument property serializer that will serialize and deserialize nothing.
 */
public class EnumArgumentPropertySerializer {

  public static final EnumArgumentPropertySerializer ENUM;

  static {
    try {
      ENUM = new EnumArgumentPropertySerializer();
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException | ClassNotFoundException |
             NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  EnumArgumentPropertySerializer() throws InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
    Constructor constructor = EnumArgumentPropertySerializer.class.getSuperclass().getDeclaredConstructor(Class.forName("net.md_5.bungee.protocol.packet.Commands$1"));
    constructor.setAccessible(true);
    constructor.newInstance(this);
  }



  protected Object read(ByteBuf buf) {
    return new EnumArgumentProperty(DefinedPacket.readString(buf));
  }

  protected void write(ByteBuf buf, Object t) {
    DefinedPacket.writeString(((EnumArgumentProperty) t).getClassName(),buf);
  }
}