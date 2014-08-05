package sami.mission;

/**
 *
 * @author nbb
 */
public class TaskSpecification implements java.io.Serializable {

    static final long serialVersionUID = 0L;
    private final String name;
    private final String taskClassName;

    public TaskSpecification(String name, String taskClassName) {
        this.name = name;
        this.taskClassName = taskClassName;
    }

    public String getName() {
        return name;
    }

    public String getTaskClassName() {
        return taskClassName;
    }

    @Override
    public String toString() {
        return name;
    }
    
    public String toVerboseString() {
        return name + "(" + taskClassName + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TaskSpecification)) {
            return false;
        }

        TaskSpecification taskSpec2 = (TaskSpecification) obj;
        if (name == null && taskSpec2.name != null) {
            return false;
        } else if (name != null && !name.equals(taskSpec2.name)) {
            return false;
        }
        if (taskClassName == null && taskSpec2.taskClassName != null) {
            return false;
        } else if (taskClassName != null && !taskClassName.equals(taskSpec2.taskClassName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + name.hashCode();
        hash = 79 * hash + taskClassName.hashCode();
        return hash;
    }
}
