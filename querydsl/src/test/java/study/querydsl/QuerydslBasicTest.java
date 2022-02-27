package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    @BeforeEach
    public void before(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1",10,teamA);
        Member member2 = new Member("member2",20,teamA);
        Member member3 = new Member("member3",30,teamB);
        Member member4 = new Member("member4",40,teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

    @Test
    public void startJPQL(){
        //member1을 찾아라
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDSL(){
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
//        QMember m = new QMember("m");
//        QMember member = QMember.member; //위에꺼랑 이거랑 똑같은 방식임
//
//        Member findMember = queryFactory
//                .select(member)
//                .from(member)
//                .where(member.username.eq("member1"))
//                .fetchOne();
//
//        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");

        //import static study.querydsl.entity.QMember.member; 이렇게 static improt해서 사용해도된다.
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        Member findMember = queryFactory
                .select(QMember.member)
                .from(QMember.member)
                .where(QMember.member.username.eq("member1").and(QMember.member.age.eq(10)))
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAnd(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        Member findMember = queryFactory
                .select(QMember.member)
                .from(QMember.member)
                .where(
                        QMember.member.username.eq("member1"),
                        (QMember.member.age.eq(10))
                )
                .fetchOne();
//and로 굳이 하지않아도 쉼표를 사용하여 가능하다
        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Member> fetch = queryFactory
                .select(member)
                .from(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        results.getTotal();
        List<Member> content = results.getResults(); //이렇게 해야 내용물을 가져올 수 있음
        for (Member member1 : content) {
            System.out.println("member =" +member1);
        }

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();

    }

    /**
     * 회원 정렬순서
     * 1.회원 나이 내림차순
     * 2.회원 이름 오름차순
     * 단 2에서 회원이름이 없으면 마지막에 출력
     */
    @Test
    public void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast()) //나이 순으로 내림차순, 이름순으로 오름차순후 null은 마지막에
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
        Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
        Assertions.assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void 페이지조회(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //시작 인덱스를 지정
                .limit(2) //한페이지에 몇개 조회할지
                .fetch();

        Assertions.assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void 전체리스트조회(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //시작 인덱스를 지정
                .limit(2) //한페이지에 몇개 조회할지
                .fetchResults();

        Assertions.assertThat(results.getTotal()).isEqualTo(4);
        Assertions.assertThat(results.getLimit()).isEqualTo(2);
        Assertions.assertThat(results.getResults().size()).isEqualTo(2);
    }

    @Test
    public void 집합(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    }

    /**
     * 팀의 이름과 각팀의 평균 연령을 구해라
     */
    @Test
    public void 그룹으로묶기() throws Exception{
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
        Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
        Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀A에 소속된 모든 회원찾기
     * @throws Exception
     */
    @Test
    public void 조인하기() throws Exception{
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        //여기서 team은 Qteam.team과 같은것이다 improt한것떄문에 이렇게 써도되는것
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        Assertions.assertThat(result.size()).isEqualTo(2);

    }

    /**
     * 회원과 팀을 조인하는데 팀이름이 teamA인 팀만 조인하고 회원은 모두 조회해라
     * @throws Exception
     */
    @Test
    public void 조인on필터링(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(member, team)//left조인이기때문에 member,team중 왼쪽의 member정보는 다가져옴
                .from(member)
                .leftJoin(member.team, team)//그냥 join을 쓰면 조건을 만족하지않는애는 그냥 안내보냄
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple="+tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void 패치조인안할떄(){
        em.flush();
        em.clear();

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("패치 조인 미적용").isFalse();
        //team 엔티티를 조인하지않았기 때문에 findmember.getTeam이 isFalse가 나오는것이 맞다 그래서
        //결과는 참으로 나오는 것
    }

    @Test
    public void 패치조인할때(){
        em.flush();
        em.clear();

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .join(member.team,team)
                .fetchJoin()
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        Assertions.assertThat(loaded).as("패치 조인 적용").isTrue();

        System.out.println(findMember.getTeam());
    }

    /**
     * 나이가 가장 많은 회원을 조회
     */
    @Test
    public void 서브쿼리(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(40);

        for (Member member1 : result) {
            System.out.println("age="+member1.getAge());
        }

    }

    /**
     * 나이가 평균 이상인 회원을 조회
     */
    @Test
    public void 서브쿼리나이평균이상(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        Assertions.assertThat(result).extracting("age")
                .containsExactly(30,40);

        for (Member member1 : result) {
            System.out.println("age="+member1.getAge());
        }

    }

    @Test
    public void select서브쿼리(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QMember memberSub = new QMember("memberSub");
        queryFactory
                .select(
                        member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

    }

    @Test
    public void 케이스문(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s= "+s);
        }
    }

    @Test
    public void 복잡한케이스(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("기다")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s= "+s);
        }
    }

    @Test
    public void 상수더하기(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("s= "+tuple);
        }
    }

    @Test
    public void 문자더하기(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))// .stringValue 자주쓰니까 알아두기
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s ="+s);
        }
    }

    @Test
    public void simpleProjection(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s =" +s);
        }
    }

    @Test
    public void tupleProjection(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username" + username);
            System.out.println("age"+ age);
        }
    }

    @Test
    public void Dto찾기ByJPQL(){
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username,m.age)" +
                        " from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto ="+memberDto);
        }
    }

    @Test
    public void Dto찾기ByQueryDsl첫번째방법(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto ="+memberDto);
        }
    }

    @Test
    public void Dto찾기ByQueryDsl두번째방법(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto ="+memberDto);
        }
    }

    @Test
    public void Dto찾기ByQueryDsl세번째방법(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto ="+memberDto);
        }
    }

    @Test
    public void Dto필드명과엔티티필드명이다를때(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();

        for (UserDto UserDto : result) {
            System.out.println("UserDto ="+UserDto);
        }
    }

    @Test
    public void findDtoByQueryProjection(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    @Test //검색 만들때 괜찮을 듯
    public void 동적쿼리_BooleanBuilder(){
        String usernameParam = "member1";
//        Integer ageParam = 10;
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);

        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        BooleanBuilder builder = new BooleanBuilder();
        if(usernameParam != null){
            builder.and(member.username.eq(usernameParam));
        }

        if(ageParam != null){
            builder.and(member.age.eq(ageParam));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void 동적쿼리_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;
//        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);

        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        return queryFactory
                .selectFrom(member)
//                .where(usernameEq(usernameParam),ageEq(ageParam))
                .where(allEq(usernameParam,ageParam)) //이렇게 한번에 할수도있다
                .fetch();
    }

    private BooleanExpression ageEq(Integer ageParam) {
        if(ageParam != null){
            return  member.age.eq(ageParam);
        }
        else {
            return null; //이렇게 where절에 null이 들어가면 조회하지않고 무시해버린다
        }
    }

    private BooleanExpression usernameEq(String usernameParam) {
        if(usernameParam != null){
            return member.username.eq(usernameParam);
        }
        else {
            return null;
        }
    }

    private BooleanExpression allEq(String usernameParam, int ageParam){
        return usernameEq(usernameParam).and(ageEq(ageParam));
    }

    @Test
    public void 벌크업데이트(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        long 비회원 = queryFactory
                .update(member)
                .set(member.username, "비회원") //조건을 만족하는 사람들의 이름을 비회원으로 바꿈
                .where(member.age.lt(28))
                .execute();//업데이트를 할때는 fetch가 아니라 excute를 쓴다

        em.flush();
        em.clear(); //벌크성 업데이트를 수행했으므로 이제 영속성 컨텍스트를 비워주는 거지 음음
    }

    @Test
    public void 벌크로더하기(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        queryFactory
                .update(member)
                .set(member.age, member.age.multiply(1))//더하기
                .execute(); //빼고싶으면 숫자에 -1넣으면된닫
    }
    
    @Test
    public void 벌크삭제(){
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member =" + member1);
        }
    }


}
