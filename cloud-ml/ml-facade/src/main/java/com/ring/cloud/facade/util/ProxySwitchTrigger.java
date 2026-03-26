package com.ring.cloud.facade.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProxySwitchTrigger {
	private static final Map<Long, AtomicBoolean> switchMap = new ConcurrentHashMap<>();

	public static void trigger(Long taskId) {
		AtomicBoolean flag = switchMap.get(taskId);
		if (flag != null) flag.set(true);
	}
	public static boolean needSwitch(Long taskId) {
		AtomicBoolean flag = switchMap.get(taskId);
		return flag != null && flag.get();
	}
	public static void reset(Long taskId) {
		AtomicBoolean flag = switchMap.get(taskId);
		if (flag != null) flag.set(false);
	}
	public static void init(Long taskId) {
		switchMap.putIfAbsent(taskId, new AtomicBoolean(false));
	}
	public static void clear(Long taskId) {
		switchMap.remove(taskId);
	}
}
