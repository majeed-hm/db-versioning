    create table PlanningTask (
        taskId smallint not null,
        OPTLOCK smallint,
        assignedUser varchar(255),
        taskIndex smallint not null,
        lastModificationDate timestamp,
        published smallint not null,
        primary key (taskId)
    );

    create index IDX_PlanningTask_assignedUser on PlanningTask(assignedUser);