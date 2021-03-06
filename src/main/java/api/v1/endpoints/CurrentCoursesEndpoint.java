package api.v1.endpoints;

import api.Endpoint;
import api.v1.ApiError;
import api.v1.SelectCourses;
import api.v1.models.Course;
import api.v1.models.Section;
import database.GetConnection;
import database.epochs.LatestCompleteEpoch;
import io.javalin.http.Handler;
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation;
import java.util.ArrayList;
import java.util.Collections;
import nyu.SubjectCode;
import nyu.Term;

public final class CurrentCoursesEndpoint extends Endpoint {

  enum SemesterCode {
    su,
    sp,
    fa,
    ja,
    current;
  }

  public String getPath() { return "/current/:semester/:school/:subject"; }

  public OpenApiDocumentation configureDocs(OpenApiDocumentation docs) {
    return docs
        .operation(openApiOperation -> {
          // openApiOperation.operationId("Operation Id");
          openApiOperation.description(
              "This endpoint returns a list of courses for a specific year, semester, school, and subject.");
          openApiOperation.summary("Courses Endpoint");
        })
        .pathParam("semester", SemesterCode.class,
                   openApiParam -> {
                     openApiParam.description("Must be a valid semester code.");
                   })
        .pathParam(
            "school", String.class,
            openApiParam -> {
              openApiParam.description(
                  "Must be a valid school code. Take a look at the docs for the schools endpoint for more information.");
            })
        .pathParam(
            "subject", String.class,
            openApiParam -> {
              openApiParam.description(
                  "Must be a valid subject code. Take a look at the docs for the subjects endpoint for more information.");
            })
        .json("400", ApiError.class,
              openApiParam -> {
                openApiParam.description(
                    "One of the values in the path parameter was not valid.");
              })
        .jsonArray("200", Course.class,
                   openApiParam -> { openApiParam.description("OK."); });
  }

  public Handler getHandler() {
    return ctx -> {
      ctx.contentType("application/json");

      Term termMut = null;
      {
        Term currentTerm = Term.getCurrentTerm();
        String semester = ctx.pathParam("semester");
        if (semester.contentEquals("current")) {
          termMut = currentTerm;
        } else {
          Integer semIntNullable = Term.semesterFromStringNullable(semester);
          if (semIntNullable == null) {
            ctx.status(400);
            ctx.json(
                new ApiError("semester code of '" + semester + "' is invalid"));
            return;
          }

          int semInt = semIntNullable;

          Term nextTerm = currentTerm.nextTerm();
          Term[] terms = new Term[] {currentTerm.prevTerm(), currentTerm,
                                     nextTerm, nextTerm.nextTerm()};

          for (Term t : terms) {
            if (t.semester == semInt) {
              termMut = t;
              break;
            }
          }
        }
      }

      Term term = termMut;
      if (term == null) { // this should never happen
        ctx.status(400);
        ctx.json(new ApiError(
            "Internal server error: valid semester code did not result in term being set"));
        return;
      }

      SubjectCode subject;
      try {
        subject =
            new SubjectCode(ctx.pathParam("subject"), ctx.pathParam("school"));
        subject.checkValid();
      } catch (IllegalArgumentException e) {
        ctx.status(400);
        ctx.json(new ApiError(e.getMessage()));
        return;
      }

      String fullData = ctx.queryParam("full");

      ctx.status(200);
      Object output = GetConnection.withConnectionReturning(conn -> {
        Integer epoch = LatestCompleteEpoch.getLatestEpoch(conn, term);
        if (epoch == null) {
          return Collections.emptyList();
        }
        if (fullData != null && fullData.toLowerCase().equals("true"))
          return SelectCourses.selectFullCourses(
              conn, epoch, Collections.singletonList(subject));
        return SelectCourses.selectCourses(conn, epoch,
                                           Collections.singletonList(subject));
      });
      ctx.json(output);
    };
  }
}
