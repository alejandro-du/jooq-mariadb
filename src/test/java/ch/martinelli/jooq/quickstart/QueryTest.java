package ch.martinelli.jooq.quickstart;

import ch.martinelli.jooq.quickstart.database.tables.records.FilmRecord;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ch.martinelli.jooq.quickstart.database.tables.Actor.ACTOR;
import static ch.martinelli.jooq.quickstart.database.tables.Category.CATEGORY;
import static ch.martinelli.jooq.quickstart.database.tables.Film.FILM;
import static ch.martinelli.jooq.quickstart.database.tables.FilmActor.FILM_ACTOR;
import static ch.martinelli.jooq.quickstart.database.tables.FilmCategory.FILM_CATEGORY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.multisetAgg;

@Transactional
@SpringBootTest
class QueryTest {

    @Autowired
    private DSLContext dsl;

    @Test
    void find_all_films() {
        Result<FilmRecord> films = dsl.selectFrom(FILM).fetch();

        assertThat(films.size()).isEqualTo(1000);
    }

    @Test
    void find_all_actors_of_horror_films() {
        Result<Record2<String, String>> actorsOfHorrorFilms = dsl
                .select(ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
                .from(ACTOR)
                .join(FILM_ACTOR).on(FILM_ACTOR.ACTOR_ID.eq(ACTOR.ACTOR_ID))
                .join(FILM).on(FILM_ACTOR.FILM_ID.eq(FILM.FILM_ID))
                .join(FILM_CATEGORY).on(FILM_CATEGORY.FILM_ID.eq(FILM.FILM_ID))
                .join(CATEGORY).on(FILM_CATEGORY.CATEGORY_ID.eq(CATEGORY.CATEGORY_ID))
                .where(CATEGORY.NAME.eq("Horror"))
                .groupBy(ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
                .orderBy(ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
                .fetch();


        assertThat(actorsOfHorrorFilms.size()).isEqualTo(155);
    }

    @Test
    void find_all_actors_of_horror_films_implicit_join() {
        Result<Record2<String, String>> actorsOfHorrorFilms = dsl
                .select(FILM_ACTOR.actor().FIRST_NAME, FILM_ACTOR.actor().LAST_NAME)
                .from(FILM_ACTOR)
                .join(FILM_CATEGORY).on(FILM_ACTOR.FILM_ID.eq(FILM_CATEGORY.FILM_ID))
                .where(FILM_CATEGORY.category().NAME.eq("Horror"))
                .groupBy(FILM_ACTOR.actor().FIRST_NAME, FILM_ACTOR.actor().LAST_NAME)
                .orderBy(FILM_ACTOR.actor().FIRST_NAME, FILM_ACTOR.actor().LAST_NAME)
                .fetch();

        assertThat(actorsOfHorrorFilms.size()).isEqualTo(155);
    }

    @Test
    void find_all_actors_of_horror_films_implicit_join_into_record() {
        List<ActorWithFirstAndLastName> actorsOfHorrorFilms = dsl
                .select(FILM_ACTOR.actor().FIRST_NAME, FILM_ACTOR.actor().LAST_NAME)
                .from(FILM_ACTOR)
                .join(FILM_CATEGORY).on(FILM_ACTOR.FILM_ID.eq(FILM_CATEGORY.FILM_ID))
                .where(FILM_CATEGORY.category().NAME.eq("Horror"))
                .groupBy(FILM_ACTOR.actor().FIRST_NAME, FILM_ACTOR.actor().LAST_NAME)
                .orderBy(FILM_ACTOR.actor().FIRST_NAME, FILM_ACTOR.actor().LAST_NAME)
                .fetchInto(ActorWithFirstAndLastName.class);

        assertThat(actorsOfHorrorFilms.size()).isEqualTo(155);
    }

    @Test
    void insert_film() {
        int insertedRows = dsl.
                insertInto(FILM)
                .columns(FILM.TITLE, FILM.LANGUAGE_ID)
                .values("Test", 1)
                .execute();

        assertThat(insertedRows).isEqualTo(1);
    }

    @Test
    void insert_film_using_record() {
        FilmRecord filmRecord = dsl.newRecord(FILM);
        filmRecord.setTitle("Test");
        filmRecord.setLanguageId(1);
        int insertedRows = filmRecord.store();

        assertThat(insertedRows).isEqualTo(1);
    }

    @Test
    void find_film() {
        FilmRecord filmRecord = dsl
                .selectFrom(FILM)
                .where(FILM.FILM_ID.eq(1))
                .fetchOne();

        assertThat(filmRecord).isNotNull();
        assertThat(filmRecord.getTitle()).isEqualTo("ACADEMY DINOSAUR");
    }

    @Test
    void find_all_actors_with_films() {
        List<ActorWithFilms> actorWithFilms = dsl
                .select(ACTOR.ACTOR_ID,
                        ACTOR.FIRST_NAME,
                        ACTOR.LAST_NAME,
                        multisetAgg(FILM_ACTOR.film().TITLE)
                                .convertFrom(r -> r.map(mapping(ActorWithFilms.FilmName::new)))
                )
                .from(ACTOR)
                .leftOuterJoin(FILM_ACTOR).on(FILM_ACTOR.ACTOR_ID.eq(ACTOR.ACTOR_ID))
                .groupBy(ACTOR.ACTOR_ID, ACTOR.FIRST_NAME, ACTOR.LAST_NAME)
                .fetch(mapping(ActorWithFilms::new));

        assertThat(actorWithFilms.size()).isEqualTo(200);
    }
}
