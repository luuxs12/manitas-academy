package com.mundomanitas.api.service;

import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.ExerciseRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Implementación del temario y el avance reales de una matrícula. */
@Service
public class StudentContentServiceImpl implements StudentContentService {

    private final CourseModuleRepository modules;
    private final VideoRepository videos;
    private final ExerciseRepository exercises;
    private final LessonProgressService lessons;

    public StudentContentServiceImpl(CourseModuleRepository modules,
                                     VideoRepository videos,
                                     ExerciseRepository exercises,
                                     LessonProgressService lessons) {
        this.modules = modules;
        this.videos = videos;
        this.exercises = exercises;
        this.lessons = lessons;
    }

    @Override
    @Transactional(readOnly = true)
    public Outline outline(UUID courseId) {
        List<ModuleOutline> result = new ArrayList<>();
        for (CourseModule module : modules.findByCourseIdOrderByPositionAsc(courseId)) {
            List<VideoOutline> classes = videos.findByModuleIdOrderByPositionAsc(module.getId())
                    .stream()
                    .map(video -> new VideoOutline(video.getId(), video.getTitle()))
                    .toList();
            List<ExerciseOutline> tasks = exercises.findByModuleIdOrderByPositionAsc(module.getId())
                    .stream()
                    .map(exercise -> new ExerciseOutline(
                            exercise.getId(), exercise.getTitle(), exercise.getPrompt()))
                    .toList();
            result.add(new ModuleOutline(module.getId(), module.getTitle(), classes, tasks));
        }
        int count = result.stream().mapToInt(module -> module.videos().size()).sum();
        return new Outline(List.copyOf(result), count);
    }

    @Override
    @Transactional(readOnly = true)
    public Progress progress(UUID enrollmentId, UUID courseId) {
        List<UUID> current = videos.findIdsByCourseId(courseId);
        Set<UUID> done = lessons.completedVideos(enrollmentId);
        int completed = (int) current.stream().filter(done::contains).count();
        int total = current.size();
        int percent = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
        return new Progress(completed, total, percent);
    }

    @Override
    @Transactional(readOnly = true)
    public Progress totals(List<Enrollment> mine) {
        int done = 0;
        int total = 0;
        for (Enrollment enrollment : mine) {
            Progress current = progress(enrollment.getId(), enrollment.getCourse().getId());
            done += current.done();
            total += current.total();
        }
        int percent = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
        return new Progress(done, total, percent);
    }
}
