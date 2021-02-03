#include <Siv3D.hpp>
#include <vector>
#include <random>
#include <stdlib.h>
#include <iostream>
#include <chrono>
#include <sys/time.h>
#include <ctime>
#include <unistd.h>
#include <stdio.h>

#define id_t unsigned int

unsigned int version = 210130;

class atom
{
public:
    String name;
    id_t id;
    s3d::Vec3 position;
    s3d::Vec3 speed;
    std::vector<id_t> link;
    double distance;
    bool operator<(const atom &another) const
    {
        return distance > another.distance;
    }
};

float getRandom()
{
    std::random_device rnd;
    return (float)(rnd()) / UINT32_MAX * 5.0f;
}

Float2 ToFloat2(Float3 v)
{
    v.x += 1.0f;
    v.y += 1.0f;
    v.x *= 0.5f * Scene::Width();
    v.y *= 0.5f;
    v.y = 1.0f - v.y;
    v.y *= Scene::Height();
    return v.xy();
}

void createAtom(String name, id_t id, std::vector<id_t> link, std::vector<atom> &atoms)
{
    atom tmp;
    tmp.name = name;
    tmp.id = id;
    tmp.link = link;
    tmp.position = Vec3(getRandom(), getRandom(), getRandom());
    tmp.speed = Vec3(0.0f, 0.0f, 0.0f);
    tmp.distance = 0.0;
    atoms.push_back(tmp);
}

Sphere getSphere(atom &drawAtom)
{
    return {drawAtom.position, 0.5};
}

void drawSphere(atom &drawAtom, const Mat4x4 &mat)
{
    Color c = Color(0xFF, 0xFF, 0xFF, 0xFF);
    if (drawAtom.name == U"green")
    {
        c = Color(0x00, 0xFF, 0x00, 0xFF);
    }
    if (drawAtom.name == U"black")
    {
        c = Color(0x00, 0x00, 0x00, 0xFF);
    }
    if (drawAtom.name == U"yellow")
    {
        c = Color(0xFF, 0xFF, 0x00, 0xFF);
    }

    getSphere(drawAtom).draw(mat, c);

    const Float3 v = SIMD::Vector3TransformCoord(SIMD_Float4(drawAtom.position, 1), mat).xyz();
    FontAsset(U"Node")(drawAtom.name)
        .drawAt(ToFloat2(v), Palette::Black);
}

void drawEdge(atom &from, atom &to, const Mat4x4 &mat)
{
    const Ray rayTo{from.position, (to.position - from.position).normalized()};
    const Vec3 end = rayTo.origin + (rayTo.intersects(getSphere(to)).value_or(1.0f) * rayTo.direction);
    const Ray rayFrom{to.position, (from.position - to.position).normalized()};
    const Vec3 begin = rayFrom.origin + (rayFrom.intersects(getSphere(from)).value_or(1.0f) * rayFrom.direction);
    {
        constexpr size_t vertexCount = 2;
        const Float3 vec[vertexCount] = {begin, end};
        Float3 out[vertexCount];
        SIMD::Vector3TransformCoordStream(out, vec, vertexCount, mat);
        Line(ToFloat2(out[0]), ToFloat2(out[1]))
            .draw(3, Palette::Black);
    }
}

int getLinkedAtom(id_t id, std::vector<atom> &atoms)
{
    int size = atoms.size();
    for (int i = 0; i < size; i++)
    {
        if (id == atoms[i].id)
            return i;
    }
}

void draw(std::vector<atom> &atoms, const Mat4x4 &mat)
{
    int size = atoms.size();
    for (int j = 0; j < size; j++)
    {
        drawSphere(atoms[j], mat);
        for (unsigned int i = 0; i < atoms[j].link.size(); i++)
        {
            int link = getLinkedAtom(atoms[j].link[i], atoms);
            if (j <= link)
                drawEdge(atoms[j], atoms[link], mat);
        }
    }
}

Vec3 forceOfRepulsion(atom &AAtom, std::vector<atom> &atoms, unsigned int now)
{
    Vec3 tmp = {0.0f, 0.0f, 0.0f};
    if (atoms.size() <= now)
    {
    }
    else if (atoms[now].id != AAtom.id)
    {
        float x = AAtom.position.x - atoms[now].position.x;
        float y = AAtom.position.y - atoms[now].position.y;
        float z = AAtom.position.z - atoms[now].position.z;

        float distance = sqrt(x * x + y * y + z * z);

        float constant_num = 1.0f;

        tmp.x += x * constant_num / distance;
        tmp.y += y * constant_num / distance;
        tmp.z += z * constant_num / distance;

        Vec3 tmp2 = forceOfRepulsion(AAtom, atoms, now + 1);
        tmp.x += tmp2.x;
        tmp.y += tmp2.y;
        tmp.z += tmp2.z;
    }
    else
    {
        Vec3 tmp2 = forceOfRepulsion(AAtom, atoms, now + 1);
        tmp.x += tmp2.x;
        tmp.y += tmp2.y;
        tmp.z += tmp2.z;
    }
    return tmp;
}

Vec3 forceOfSpring(atom &Atom, std::vector<atom> &atoms)
{
    Vec3 tmp = {0.0f, 0.0f, 0.0f};
    for (unsigned int i = 0; i < Atom.link.size(); i++)
    {
        atom link = atoms[getLinkedAtom(Atom.link[i], atoms)];
        float x = link.position.x - Atom.position.x;
        float y = link.position.y - Atom.position.y;
        float z = link.position.z - Atom.position.z;
        float distance = sqrt(x * x + y * y + z * z);
        float goalDistance = 2.0f;
        float SPconst = 100.0f;
        float f = SPconst * (distance - goalDistance);

        tmp.x += x / distance * f;
        tmp.y += y / distance * f;
        tmp.z += z / distance * f;
    }
    return tmp;
}

long getTime()
{
    auto millisec_since_epoch = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();
    return millisec_since_epoch;
}

long nowTime;
long oldTime;

void move(std::vector<atom> &atoms)
{
    oldTime = nowTime;
    nowTime = getTime();
    float Dtime = (nowTime - oldTime) / 1000.0f;

    int size = atoms.size();
    for (int j = 0; j < size; j++)
    {
        Vec3 force = {0.0f, 0.0f, 0.0f};
        Vec3 tmp1 = forceOfRepulsion(atoms[j], atoms, 0);
        force.x += tmp1.x;
        force.y += tmp1.y;
        force.z += tmp1.z;
        tmp1 = forceOfSpring(atoms[j], atoms);
        force.x += tmp1.x;
        force.y += tmp1.y;
        force.z += tmp1.z;

        atoms[j].speed.x = (atoms[j].speed.x + Dtime * force.x) * 0.9f;
        atoms[j].speed.y = (atoms[j].speed.y + Dtime * force.y) * 0.9f;
        atoms[j].speed.z = (atoms[j].speed.z + Dtime * force.z) * 0.9f;

        atoms[j].position.x += Dtime * atoms[j].speed.x;
        atoms[j].position.y += Dtime * atoms[j].speed.y;
        atoms[j].position.z += Dtime * atoms[j].speed.z;
    }
}

Vec4 calcVector(Vec3 &focusPosition, Vec3 &eyePosition)
{
    Vec4 vect = {0.0, 0.0, 0.0, 0.0};
    vect.x = focusPosition.x - eyePosition.x;
    vect.y = focusPosition.y - eyePosition.y;
    vect.z = focusPosition.z - eyePosition.z;
    vect.w = -(vect.x * eyePosition.x + vect.y * eyePosition.y + vect.z * eyePosition.z);
    return vect;
}

double calcDistance(Vec3 &atomPosition, Vec4 &vect)
{
    return std::abs(vect.x * atomPosition.x + vect.y * atomPosition.y + vect.z * atomPosition.z + vect.w) / std::sqrt(vect.x * vect.x + vect.y * vect.y + vect.z * vect.z);
}

void readJSON(String file, std::vector<atom> &atoms)
{
    JSONReader json(file);
    unsigned int constnum = 0;

    for (const auto &object : json.objectView())
    {
        if (object.name == U"atoms")
        {
            for (const auto &object5 : object.value.arrayView())
            {
                id_t id;
                String name;
                std::vector<id_t> link;
                for (const auto &object2 : object5.objectView())
                {
                    if (object2.name == U"id")
                        id = object2.value.get<double>();
                    if (object2.name == U"name")
                        name = object2.value.getString();
                    if (object2.name == U"links")
                    {
                        for (const auto &object6 : object2.value.arrayView())
                        {
                            int attr;
                            for (const auto &object3 : object6.objectView())
                            {
                                if (object3.name == U"attr")
                                {
                                    attr = object3.value.get<double>();
                                }
                                if (object3.name == U"data")
                                {
                                    if ((attr & 0x80) != 0x00)
                                    {
                                        switch (attr)
                                        {
                                        case 0x80:
                                        case 0x81:
                                        case 0x85:
                                            createAtom(Format(object3.value.get<double>()), 0x8000 | constnum, {id}, atoms);
                                            break;
                                        case 0x82:
                                        case 0x83:
                                        case 0x84:
                                        case 0x8a:
                                            createAtom(object3.value.getString(), 0x8000 | constnum, {id}, atoms);
                                            break;
                                        }
                                        link.push_back(0x8000 | constnum);
                                        constnum++;
                                    }
                                    else
                                    {
                                        link.push_back(object3.value.get<double>());
                                    }
                                }
                            }
                        }
                        createAtom(name, id, link, atoms);
                    }
                }
            }
        }
    }
}

int SLIM()
{
    char *slim = getenv("LMNTAL_HOME");
    std::string slimpath = slim;
    slimpath += "/installed/bin/slim";
    std::cout << "slimpath: " << slimpath << std::endl;
    char lnmprogram[] = "/home/suan/Downloads/dna.lmn";
    //char lnmprogram[] = "/home/suan/ダウンロード/LaViT2_9_1/demo/unyo/frog.lmn";
    int pipes[2];
    if (pipe(pipes) < 0)
    {
        perror("pipe");
        exit(-1);
    }

    pid_t pid = fork();
    if (pid == 0) //child
    {
        close(1);
        dup(pipes[1]);
        close(pipes[0]);
        close(pipes[1]);
        execlp(slimpath.c_str(), "-t", "--dump-json", "--hl", "--hl", "--use-builtin-rule", lnmprogram, NULL);
    }
    else //parent
    {
        close(pipes[1]);
    }

    return pipes[0];
}

void WB(FILE *PIPEIN, char tmp[81920], int *hasnext)
{
    if (*hasnext)
    {
        fscanf(PIPEIN, "%s", tmp);
        FILE *JSON = fopen("./a.json", "w");
        fprintf(JSON, "%s", tmp);
        fclose(JSON);
    }
}

void Main()
{
    int pipes = SLIM();
    int hasnext = 1;
    char tmp[81920];
    FILE *PIPEIN = fdopen(pipes, "r");

    WB(PIPEIN, tmp, &hasnext);

    s3d::Window::SetTitle(U"Graphene 3D (version: " + Format(version) + U")");

    std::vector<atom> atoms = {};

    Scene::SetBackground(ColorF{0.8, 0.9, 1.0});
    FontAsset::Register(U"Node", 22, Typeface::Bold);

    constexpr double fov = 45_deg;
    Vec3 focusPosition{0.0, 0.0, 0.0};
    Vec3 eyePosition{10.0, 0.0, 0.0};
    Vec3 vector2{0.0, 0.0, 0.0};
    BasicCamera3D camera{Scene::Size(), fov, eyePosition, focusPosition};

    readJSON(U"./a.json", atoms);

    nowTime = getTime();
    oldTime = nowTime;

    float XY = 0;
    float XZ = 0;
    float movespeed = 10.0f;
    float defaultmovespeed = 10.0f;
    unsigned char A = 0;

    while (System::Update())
    {
        float Dtime = (nowTime - oldTime) / 1000.0f;
        vector2.x = focusPosition.x - eyePosition.x;
        vector2.z = focusPosition.z - eyePosition.z;
        float distance = sqrt(vector2.x * vector2.x + vector2.z * vector2.z);
        vector2.x = vector2.x / distance;
        vector2.z = vector2.z / distance;

        if ((A & 0x3F) == 0x00)
        {
            Vec4 vect = calcVector(focusPosition, eyePosition);
            int size = atoms.size();
            for (int i = 0; i < size; i++)
            {
                atoms[i].distance = calcDistance(atoms[i].position, vect);
            }
            std::sort(atoms.begin(), atoms.end());
        }

        if (s3d::KeyControl.pressed())
        {
            movespeed = defaultmovespeed * 10;
        }
        else
        {
            movespeed = defaultmovespeed;
        }
        if (s3d::MouseL.pressed())
        {
            XY += Cursor::Delta().y * 0.002;
            XZ += Cursor::Delta().x * 0.002;
            focusPosition.x = eyePosition.x + 10 * std::cos(XZ) * std::cos(XY);
            focusPosition.y = eyePosition.y + 10 * std::sin(XY);
            focusPosition.z = eyePosition.z + 10 * std::sin(XZ) * std::cos(XY);
        }
        if (s3d::KeyLShift.pressed())
        {
            eyePosition.y += movespeed * Dtime;
            focusPosition.y += movespeed * Dtime;
        }
        if (s3d::KeySpace.pressed())
        {
            eyePosition.y -= movespeed * Dtime;
            focusPosition.y -= movespeed * Dtime;
        }
        if (s3d::KeyS.pressed())
        {
            eyePosition.x += movespeed * vector2.x * Dtime;
            eyePosition.z += movespeed * vector2.z * Dtime;
            focusPosition.x += movespeed * vector2.x * Dtime;
            focusPosition.z += movespeed * vector2.z * Dtime;
        }
        if (s3d::KeyW.pressed())
        {
            eyePosition.x -= movespeed * vector2.x * Dtime;
            eyePosition.z -= movespeed * vector2.z * Dtime;
            focusPosition.x -= movespeed * vector2.x * Dtime;
            focusPosition.z -= movespeed * vector2.z * Dtime;
        }
        if (s3d::KeyD.pressed())
        {
            eyePosition.x -= movespeed * vector2.z * Dtime;
            eyePosition.z += movespeed * vector2.x * Dtime;
            focusPosition.x -= movespeed * vector2.z * Dtime;
            focusPosition.z += movespeed * vector2.x * Dtime;
        }
        if (s3d::KeyA.pressed())
        {
            eyePosition.x += movespeed * vector2.z * Dtime;
            eyePosition.z -= movespeed * vector2.x * Dtime;
            focusPosition.x += movespeed * vector2.z * Dtime;
            focusPosition.z -= movespeed * vector2.x * Dtime;
        }
        if (s3d::KeyR.pressed())
        {
            int size = atoms.size();
            for (int i = 0; i < size; i++)
            {
                atoms[i].position.x = getRandom();
                atoms[i].position.y = getRandom();
                atoms[i].position.z = getRandom();
                atoms[i].speed.x = 0.0;
                atoms[i].speed.y = 0.0;
                atoms[i].speed.z = 0.0;
            }
        }
        if (s3d::KeyT.pressed())
        {
            focusPosition.x = 0.0;
            focusPosition.y = 0.0;
            focusPosition.z = 0.0;
            eyePosition.x = 10.0;
            eyePosition.y = 0.0;
            eyePosition.z = 0.0;
        }

        // std::cout << s3d::KeyLShift.pressed() << " " << s3d::KeySpace.pressed() << std::endl;
        // std::cout << eyePosition.x << " " << eyePosition.y << " " << eyePosition.z << std::endl;
        // std::cout << focusPosition.x << " " << focusPosition.y << " " << focusPosition.z << std::endl;
        camera.setView(eyePosition, focusPosition);
        const Mat4x4 mat = camera.getMat4x4();

        draw(atoms, mat);
        move(atoms);

        A++;
    }
}