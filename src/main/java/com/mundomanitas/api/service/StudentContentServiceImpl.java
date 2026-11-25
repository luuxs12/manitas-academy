package com.mundomanitas.api.service;

import com.mundomanitas.api.model.CourseModule;
import com.mundomanitas.api.model.Enrollment;
import com.mundomanitas.api.repository.CourseModuleRepository;
import com.mundomanitas.api.repository.ExerciseRepository;
import com.mundomanitas.api.repository.VideoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Implementación del temario y el avance reales de una matrícula. */
@Service
public class StudentContentServiceImpl implements StudentContentService {

    private static final Logger log = LoggerFactory.getLogger(StudentContentServiceImpl.class);

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
    public Map<UUID, Progress> progressFor(List<Enrollment> mine) {
        if (mine.isEmpty()) {
            return Map.of();
        }
        Set<UUID> enrollmentIds = mine.stream()
                .map(Enrollment::getId)
                .collect(Collectors.toSet());
        Set<UUID> courseIds = mine.stream()
                .map(enrollment -> enrollment.getCourse().getId())
                .collect(Collectors.toSet());

        Map<UUID, Set<UUID>> videosByCourse = new HashMap<>();
        for (VideoRepository.CourseVideo row : videos.findCourseVideos(courseIds)) {
            videosByCourse.computeIfAbsent(row.getCourseId(), ignored -> new HashSet<>())
                    .add(row.getVideoId());
        }
        Map<UUID, Set<UUID>> doneByEnrollment = lessons.completedVideosFor(enrollmentIds);

        Map<UUID, Progress> result = new HashMap<>();
        for (Enrollment enrollment : mine) {
            UUID id = enrollment.getId();
            Set<UUID> current = videosByCourse.getOrDefault(enrollment.getCourse().getId(), Set.of());
            Set<UUID> done = doneByEnrollment.getOrDefault(id, Set.of());
            int completed = (int) current.stream().filter(done::contains).count();
            int total = current.size();
            int percent = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
            result.put(id, new Progress(completed, total, percent));
        }
        log.info("Avance calculado en lote para {} matrículas y {} cursos",
                mine.size(), courseIds.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Progress totals(List<Enrollment> mine) {
        int done = 0;
        int total = 0;
        for (Progress current : progressFor(mine).values()) {
            done += current.done();
            total += current.total();
        }
        int percent = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
        return new Progress(done, total, percent);
    }
}
