# Data-Flow Compiler Plugin
The Data-Flow plugin is a Kotlin compiler plugin designed to reduce boilerplate when working with StateFlow in Jetpack Compose or other reactive frameworks.
It automatically generates a DataFlow class that wraps a data class, providing individual StateFlows for each property and an "all" state for the entire object.

# Features
- Automatic State Generation: For a given data class, it generates a DataFlow wrapper.
- Individual Property Flows: Exposes a MutableStateFlow for each property of the data class.
- Aggregate "all" Flow: Provides a single StateFlow that emits the entire data class object whenever any property changes.
- Update Functions: Generates updateX() methods for easy, atomic updates of individual properties.

# Usage

1. Apply the plugin

First, apply the plugin in your module's build.gradle.kts file.

```kotlin
plugins {
    id("io.fergdev.dataflow")
}
```

2. Annotate Your Data Class

Add the @DataFlow annotation to any data class you want to transform.

```kotlin
import io.fergdev.dataflow.DataFlow

@DataFlow
data class Person(val name: String, val age: Int)
```
3. Use the Generated DataFlow Class

The plugin will generate a nested DataFlow class inside Person. You can instantiate it and use its properties directly in your code (e.g., in a ViewModel).

### Example 1: Basic Instantiation and Updates

```kotlin
// The compiler generates `Person.DataFlow` for you
val personDataFlow = Person.DataFlow(name = "John Doe", age = 30)// Access individual state flows
val nameFlow: StateFlow<String> = personDataFlow.name
val ageFlow: StateFlow<Int> = personDataFlow.age
println(nameFlow.value) // --> "John Doe"

// Use the generated update functions
personDataFlow.updateAge(31)
println(personDataFlow.age.value) // --> 31

// The 'all' flow is automatically updated
val currentPerson: Person = personDataFlow.all.value
println(currentPerson) // --> Person(name="John Doe", age=31)
```
### Example 2: Usage in a Jetpack Compose ViewModel

The DataFlow class is perfect for managing UI state.

```kotlin
class ProfileViewModel : ViewModel() {

    // Instantiate the generated DataFlow class with initial values
    private val _person = Person.DataFlow(name = "Jane Doe", age = 25)

    // Expose the immutable StateFlows to the UI
    val name: StateFlow<String> = _person.name
    val age: StateFlow<Int> = _person.age
    val person: StateFlow<Person> = _person.all

    fun onNameChange(newName: String) {
        viewModelScope.launch {
            // The generated function updates both the individual flow
            // and the 'all' flow atomically.
            _person.updateName(newName)
        }
    }
}

// In your Composable
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val name by viewModel.name.collectAsState()
    val age by viewModel.age.collectAsState()

    Column {
        TextField(value = name, onValueChange = { viewModel.onNameChange(it) })
        Text("Age: $age")
    }
}
```
# IDE Support
The K2 Kotlin IntelliJ plugin supports running third party FIR plugins in the IDE, but this feature is hidden behind a flag. 
Some DataFlow features can take advantage of this, namely diagnostic reporting directly in the IDE and some opt-in features to see generated declarations.

To enable it, do the following:

- Enable K2 Mode for the Kotlin IntelliJ plugin.
- Open the Registry
- Set the kotlin.k2.only.bundled.compiler.plugins.enabled entry to false.
- 
Note that support is unstable and subject to change.
