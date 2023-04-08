package com.xtracr.realcamera.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import net.minecraftforge.fml.ModList;

public class ModUtils {
    
    public static boolean isLoaded(final String modID) {
        return ModList.get().isLoaded(modID);
    }

    public static Optional<Class<?>> getClass(final String className) {
        try {
			return Optional.of(Class.forName(className));
		} catch (ClassNotFoundException exception) {
			return Optional.empty();
		}
    }

	public static Optional<Field> getField(final Optional<Class<?>> classObj, final String fieldName) {
		return classObj.map(cls -> {
			try {
				final Field fld = cls.getDeclaredField(fieldName);
				fld.setAccessible(true);
				return fld;
			} catch (NoSuchFieldException | SecurityException exception) {
				return null;
			}
		});
	}

    public static Optional<Object> getFieldValue(final Optional<Field> field, final Object object) {
        return field.map(fld -> {
            try {
                return fld.get(object);
            } catch (IllegalArgumentException | IllegalAccessException exception) {
                return null;
            }
        });
    }

    public static Optional<Object> getFieldValue(final Optional<Class<?>> classObj, final String fieldName, final Object obj) {
        return getField(classObj, fieldName).map(fld -> {
            try {
                return fld.get(obj);
            } catch (IllegalArgumentException | IllegalAccessException exception) {
                return null;
            }
        });
    }

    public static void setField(final Optional<Field> field, final Object object, Object value) {
        field.ifPresent(fld -> {
            try {
                fld.set(object, value);
            } catch (IllegalArgumentException | IllegalAccessException exception) {
                
            }
        });
    }

    public static void setField(final Optional<Class<?>> classObj, final String fieldName, final Object object, Object value) {
		getField(classObj, fieldName).ifPresent(fld -> {
			try {
				fld.set(object, value);
			} catch (IllegalArgumentException | IllegalAccessException exception) {
				
			}
		});
	}

    public static Optional<Method> getMethod(final Optional<Class<?>> classObj, final String methodName, Class<?>... args) {
		return classObj.map(cls -> {
			try {
				final Method mtd = cls.getMethod(methodName, args);
				mtd.setAccessible(true);
				return mtd;
			} catch (NoSuchMethodException | SecurityException exception) {
			    return null;
			}
		});
	}
	
}
