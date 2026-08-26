# Test Conventions

## File Naming

- Test class: `{ClassName}Test.java`
- Location: mirrors `src/main/java/` structure under `src/test/java/`

```
src/main/java/.../application/usecase/CreateDistributionConfigUseCase.java
src/test/java/.../application/usecase/CreateDistributionConfigUseCaseTest.java
```

## Test Method Naming

Pattern: `methodName_condition_expectedBehavior`

```java
@Test
void execute_validRequest_createsConfig() { ... }

@Test
void execute_configNotFound_throwsException() { ... }
```

## Test Structure

Use Arrange-Act-Assert (AAA):

```java
@ExtendWith(MockitoExtension.class)
class CreateDistributionConfigUseCaseTest {

    @Mock
    private DistributionConfigJPARepository repository;
    @Mock
    private DistributionConfigMapper mapper;
    @InjectMocks
    private CreateDistributionConfigUseCase useCase;

    @Test
    void execute_validRequest_createsConfig() {
        // Arrange
        var request = new CreateDistributionConfigRequest("Name", "BRW01", poolConfig);
        when(mapper.toEntity(any())).thenReturn(savedEntity);
        when(repository.save(savedEntity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(domain);

        // Act
        var result = useCase.execute(request);

        // Assert
        assertThat(result.name()).isEqualTo("Name");
    }
}
```

## Libraries

| Library | Purpose |
|---|---|
| JUnit 5 | Test framework |
| Mockito | Mocking (`@Mock`, `@InjectMocks`, `when/verify`) |
| AssertJ | Fluent assertions (`assertThat(...)`) |

## Test Environment

Tests use an **H2 in-memory database** (configured in `src/test/resources/application.properties`)
con `MODE=MySQL` para emular sintaxis MySQL.
