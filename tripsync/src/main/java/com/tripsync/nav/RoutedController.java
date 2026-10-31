package com.tripsync.nav;

/**
 * 화면 전환 시 파라미터를 전달받고 싶은 컨트롤러가 구현하는 인터페이스.
 *
 * <p>예: 모임 목록에서 모임 카드를 누르면 {@code Router.navigate(Route.GROUP_DETAIL, groupId)} 로
 * 이동하고, 모임 상세 컨트롤러는 {@link #onEnter(Object)} 에서 그 groupId 를 받는다.</p>
 *
 * <p>FXML 의 {@code initialize()} 는 화면이 만들어질 때 한 번 호출되는데, 그 시점에는
 * 아직 파라미터가 없다. 그래서 파라미터가 필요한 초기화는 {@code initialize()} 가 아니라
 * {@code onEnter()} 안에서 해야 한다.</p>
 */
public interface RoutedController {

    /**
     * 화면이 실제로 표시되기 직전에 호출된다.
     *
     * @param param 이전 화면이 넘겨준 값. 넘긴 값이 없으면 {@code null}
     */
    void onEnter(Object param);
}
