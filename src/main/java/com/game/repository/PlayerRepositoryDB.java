package com.game.repository;

import com.game.entity.Player;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import javax.annotation.PreDestroy;
import java.util.Properties;
import java.util.List;
import java.util.Optional;

@Repository(value = "db")
public class PlayerRepositoryDB implements IPlayerRepository {
    private final SessionFactory sessionFactory;

    public PlayerRepositoryDB() {

        Properties properties = new Properties();

        properties.put(Environment.DRIVER, "com.p6spy.engine.spy.P6SpyDriver");
        properties.put(Environment.URL, "jdbc:p6spy:mysql://localhost:3306/rpg");
        properties.put(Environment.DIALECT, "org.hibernate.dialect.MySQL8Dialect");
        properties.put(Environment.SHOW_SQL, true);
        properties.put(Environment.USER, "root");
        properties.put(Environment.PASS, "123456789");
        properties.put(Environment.HBM2DDL_AUTO, "update");

        Configuration configuration = new Configuration();
        configuration.setProperties(properties);
        configuration.addAnnotatedClass(Player.class);

        sessionFactory = configuration.buildSessionFactory();
    }

    @Override
    public List<Player> getAll(int pageNumber, int pageSize) {

        try (Session session = sessionFactory.openSession()) {

            String sql = "SELECT * FROM rpg.player LIMIT :pageSize OFFSET :offset";
            NativeQuery<Player> query = session.createNativeQuery(sql, Player.class);
            int calculatedOffset = pageNumber * pageSize;

            query.setParameter("pageSize", pageSize);
            query.setParameter("offset", calculatedOffset);

            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public int getAllCount() {

        Session session = sessionFactory.openSession();

        try {
            Long count = session
                    .createNamedQuery("Player.length", Long.class)
                    .getSingleResult();

            return count.intValue();

        } finally {
            session.close();
        }
    }

    @Override
    public Player save(Player player) {
        return saveOrUpdate(player);
    }

    @Override
    public Player update(Player player) {
        return saveOrUpdate(player);
    }

    @Override
    public Optional<Player> findById(long id) {
        try (Session session = sessionFactory.openSession()) {
            Player player = session.get(Player.class, id);
            return Optional.ofNullable(player);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void delete(Player player) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            // Сначала переводим объект в состояние managed
            Object managedPlayer = session.merge(player);
            session.remove(managedPlayer);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    @PreDestroy
    public void beforeStop() {
        sessionFactory.close();
    }

    private Player saveOrUpdate(Player player) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            session.evict(player);
            Player mergedPlayer = (Player) session.merge(player);

            transaction.commit();
            return mergedPlayer;
        } catch (Exception e) {
            if (transaction != null && transaction.getStatus().canRollback()) {
                transaction.rollback();
            }
            e.printStackTrace();
            throw e;
        }
    }
}