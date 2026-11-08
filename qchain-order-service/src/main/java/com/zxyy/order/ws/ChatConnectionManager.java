package com.zxyy.order.ws;

import jakarta.websocket.Session;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 这里类内部使用ConcurrentHashMap 线程安全的map集合维护 连接关系
 * set集合使用的也是线程安全的 ConcurrentHashMap.newKeySet()
 * compute
 */
@Component
public class ChatConnectionManager {

    // 订单号 → 买家连接集合
    private final Map<String, Set<Session>> buyerConnections =
            new ConcurrentHashMap<>();

    // 管理员ID → 管理员连接集合
    private final Map<Long, Set<Session>> adminConnections =
            new ConcurrentHashMap<>();

    // 订单号 → 负责管理员ID
    private final Map<String, Long> orderAdmins =
            new ConcurrentHashMap<>();

    // 登记买家连接：订单密码验证成功后调用
    public void registerBuyer(String orderNo, Session session) {
        buyerConnections.compute(orderNo, (key, sessions) -> {
            if (sessions == null) {
                sessions = ConcurrentHashMap.newKeySet();
            }
            sessions.add(session);
            return sessions;
        });
    }

    // 登记管理员连接：管理员身份验证成功后调用
    public void registerAdmin(Long adminId, Session session) {
        adminConnections.compute(adminId, (key, sessions) -> {
            if (sessions == null) {
                sessions = ConcurrentHashMap.newKeySet();
            }
            sessions.add(session);
            return sessions;
        });
    }

    // 移除关闭的买家连接
    public void removeBuyer(String orderNo, Session session) {
        buyerConnections.computeIfPresent(orderNo, (key, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    // 移除关闭的管理员连接
    public void removeAdmin(Long adminId, Session session) {
        adminConnections.computeIfPresent(adminId, (key, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    // 查询订单当前的买家连接，返回一份集合快照
    public Set<Session> getBuyerSessions(String orderNo) {
        Set<Session> sessions = buyerConnections.get(orderNo);
        return sessions == null ? Set.of() : Set.copyOf(sessions);
    }

    // 查询管理员当前的连接
    public Set<Session> getAdminSessions(Long adminId) {
        Set<Session> sessions = adminConnections.get(adminId);
        return sessions == null ? Set.of() : Set.copyOf(sessions);
    }

    // 查询已登记连接的管理员，用于通知待接待订单
    public Set<Long> getConnectedAdminIds() {
        return Set.copyOf(adminConnections.keySet());
    }

    // 管理员接待订单：已经被其他管理员接待时返回 false
    public boolean bindOrderAdmin(String orderNo, Long adminId) {
        Long existing = orderAdmins.putIfAbsent(orderNo, adminId);
        return existing == null || existing.equals(adminId);
    }

    // 查询订单由哪位管理员负责；null 表示尚未接待
    public Long getOrderAdmin(String orderNo) {
        return orderAdmins.get(orderNo);
    }

    // 结束接待：只有绑定的管理员ID匹配时才移除
    public boolean unbindOrderAdmin(String orderNo, Long adminId) {
        return orderAdmins.remove(orderNo, adminId);
    }

    //查询某个订单是否绑定了管理员
    public Long selectAdminByOrderNo(String OrderNo){
        return orderAdmins.get(OrderNo);
    }
}