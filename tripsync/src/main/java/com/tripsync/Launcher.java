package com.tripsync;

/**
 * IDE 에서 바로 실행하기 위한 보조 진입점.
 *
 * <p>{@code Application} 을 상속한 클래스를 IDE 에서 곧바로 실행하면 JavaFX 가
 * "JavaFX runtime components are missing" 오류를 낸다. JavaFX 런타임을 모듈 경로로
 * 넘겨주지 않았기 때문인데, {@code Application} 을 상속하지 '않은' 클래스의 main 을
 * 거쳐 실행하면 이 검사를 지나가므로 VM 옵션을 따로 설정하지 않아도 된다.</p>
 *
 * <p>팀원별 실행 방법:
 * <ul>
 *   <li>터미널: {@code mvn clean javafx:run}</li>
 *   <li>IntelliJ / Eclipse: 이 {@code Launcher} 클래스를 우클릭해 Run</li>
 * </ul>
 */
public class Launcher {

    public static void main(String[] args) {
        TripSyncApp.main(args);
    }
}
