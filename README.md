# Querydsl

●sql 비교표현식(member.username과 member1을 비교)
> eq - equal ( = ) username = ‘member1’
> 
> ne - not equal ( <> ) username != ‘member1’ 이런식
> 
> lt - little ( < )
> 
> loe - little or equal ( <= )
> 
> gt - greater ( > )
> 
> goe - greater or equal ( >= )

> Like 사용 -> member.username.like(“member%”)원하는곳에 %사용

> Contains사용 ->member.username.contains(“member”) 이러면 %member%로 인식

> StartsWith사용 -> member.username.startsWith 이러면 member%로 인식

●queryDsl 결과 조회
1. fetch() : 리스트를 조회하는 것, 데이터가 없으면 빈 리스트를 반환
2. fetchOne() : 하나의 결과만 조회, 결과가 없으면 null, 결과가 두 개이상이면 exception이 터진다
3. fetchFirst() : limit(1).fetchOne() 과 동일한 것 맨 처음의 한 개만 가져온다는 의미
4. fetchResults() : 페이징 정보를 포함하여 total count쿼리를 실행함, 조회한 리스트 + 전체 개수
5. fetchCount() : count쿼리로 변경해서 count수만 조회하는 것, 몇 개인지만 조회한다는 것

●queryDsl 조인하기

1. 조인의 기본 문법은 첫 번째 파라미터에 조인 대상을 지정하고 두 번째 파라미터에 별칭으로 사용할 Q타입을 지정하면 된다.
	
@QueryProjection

내가 사용할 Dto 생성자에다가 붙이는 어노테이션으로
이것을 붙인후 compileQuerydsl을 실행하면 dto도 Q타입으로 생성이 된다.

queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
		
이렇게 생성하여 사용할 수 있다.


●QueryDsl을 이용하여 더하기 곱하기 하기

queryFactory
                .update(member)
                .set(member.age, member.age.add(1)) //더하기
			          member.age.multiply //곱하기
                .execute(); 
		
//빼고싶으면 숫자에 -1넣으면된다
