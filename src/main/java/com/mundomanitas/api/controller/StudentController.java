package com.mundomanitas.api.controller;

import com.mundomanitas.api.model.Course;
import com.mundomanitas.api.service.CourseCatalog;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Panel de la persona autenticada.
 *
 * <p>Con el plan gratuito el panel muestra los cursos abiertos y deja a la
 * vista cuántos quedan detrás de la membresía. El progreso arranca en cero:
 * todavía no hay lecciones ni seguimiento de avance.</p>
 */
@Controller
public class StudentController {

    private final CourseCatalog catalog;

    public StudentController(CourseCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/panel")
    public String panel(
            @RequestParam(name = "bienvenida", required = false) String welcome,
            Model model
    ) {
        List<Course> all = catalog.all();
        List<Course> myCourses = all.stream().filter(Course::free).toList();

        model.addAttribute("myCourses", myCourses);
        model.addAttribute("nextCourse", myCourses.isEmpty() ? null : myCourses.get(0));
        model.addAttribute("activeCourses", myCourses.size());
        model.addAttribute("lockedCourses", all.size() - myCourses.size());
        model.addAttribute("welcome", welcome != null);
        return "student/panel";
    }
}
